const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ts = () => new Date(Date.now() - 1000);

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated owner can create and update preferences, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      lang: "ne",
      displayCurrency: "NPR",
      hideBalances: false,
      appPin: "",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      lang: "en",
      updatedAt: ts(),
    })
  );

  // Shadow update with ghost field denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lang: "en",
      isAdmin: true,
      updatedAt: ts(),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lang: "en",
      appPin: "1234",
      updatedAt: ts(),
    })
  );
});

test("UserAccount CRUD and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1");
  await assertSucceeds(
    accRef.set({
      userId: ALICE_UID,
      catalogId: "esewa",
      customLabel: "eSewa Wallet",
      balanceNpr: 1500.5,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // List own accounts succeeds; Bob listing Alice's accounts fails
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("accounts").get());

  // Update balance succeeds; updating catalogId fails
  await assertSucceeds(accRef.update({ balanceNpr: 2500.0, updatedAt: ts() }));
  await assertFails(accRef.update({ catalogId: "khalti", updatedAt: ts() }));
});

test("LedgerTransaction is append-only and enforces schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");

  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      refNo: "MH-261116113500",
      type: "EXPENSE",
      title: "Groceries",
      category: "🛒 किराना सामान (Groceries)",
      amountNpr: 450.0,
      originalAmount: 450.0,
      originalCurrency: "NPR",
      accountId: "acc_1",
      prevBalanceNpr: 2500.0,
      newBalanceNpr: 2050.0,
      adTimestamp: "2026-11-16 11:35:00",
      bsDate: "2083-08-01",
      bsYear: 2083,
      bsMonth: 8,
      note: "Local shop",
      createdAt: ts(),
    })
  );

  // Update and delete denied on append-only transactions
  await assertFails(txRef.update({ amountNpr: 100.0 }));
  await assertFails(txRef.delete());
});

test("KhataRecord paid/pending transitions, due date reminders, and strict schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const khataRef = aliceDb.collection("users").doc(ALICE_UID).collection("khatas").doc("khata_1");

  await assertSucceeds(
    khataRef.set({
      userId: ALICE_UID,
      type: "LEND",
      partyName: "Ram",
      itemDetail: "Cash lend",
      amountNpr: 1000.0,
      bsDate: "2083-08-01",
      adTimestamp: "2026-11-16 11:35:00",
      settled: false,
      dueBsDate: "2083-08-15",
      dueAdDate: "2026-11-30",
      reminderEnabled: true,
      notes: "Return before month end",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  // Mark Paid succeeds
  await assertSucceeds(
    khataRef.update({
      settled: true,
      paidBsDate: "2083-08-10",
      paidAdDate: "2026-11-25",
      updatedAt: ts(),
    })
  );

  // Mark back to Pending succeeds
  await assertSucceeds(
    khataRef.update({
      settled: false,
      paidBsDate: "",
      paidAdDate: "",
      updatedAt: ts(),
    })
  );

  // Update due date reminder & notes succeeds
  await assertSucceeds(
    khataRef.update({
      dueBsDate: "2083-08-20",
      dueAdDate: "2026-12-05",
      reminderEnabled: false,
      notes: "Rescheduled",
      updatedAt: ts(),
    })
  );

  // Modifying immutable fields (amountNpr, type, userId) or ghost fields fails
  await assertFails(khataRef.update({ amountNpr: 500.0, updatedAt: ts() }));
  await assertFails(khataRef.update({ type: "BORROW", updatedAt: ts() }));
  await assertFails(khataRef.update({ reminderEnabled: true, extraField: "bad", updatedAt: ts() }));

  // Cross-user update and delete fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("khatas").doc("khata_1").update({
    settled: true,
    updatedAt: ts(),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("khatas").doc("khata_1").delete());

  // Owner delete succeeds
  await assertSucceeds(khataRef.delete());
});

test("SavingsGoal and AutoRecurringRule validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1");
  const ruleRef = aliceDb.collection("users").doc(ALICE_UID).collection("recurring_rules").doc("rule_1");

  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "Motorcycle",
      targetNpr: 500000.0,
      savedNpr: 0.0,
      targetBsDate: "2084-12-30",
      frequency: "MONTHLY",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(goalRef.update({ savedNpr: 10000.0, updatedAt: ts() }));

  await assertSucceeds(
    ruleRef.set({
      userId: ALICE_UID,
      type: "INCOME",
      title: "Salary",
      category: "💵 मासिक तलब (Monthly Salary)",
      amountNpr: 45000.0,
      accountId: "acc_1",
      frequency: "MONTHLY",
      lastRunAdDate: "2026-11-16",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(ruleRef.update({ lastRunAdDate: "2026-12-16", updatedAt: ts() }));
});
