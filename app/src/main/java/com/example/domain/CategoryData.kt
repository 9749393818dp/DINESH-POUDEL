package com.example.domain

data class CategoryItem(
  val icon: String,
  val name: String,
) {
  val label: String get() = "$icon $name"
}

object CategoryData {
  val expenseGroups: Map<String, List<CategoryItem>> = linkedMapOf(
    "१. भान्सा र खानपिन" to listOf(
      CategoryItem("🌾", "चामल (Rice)"),
      CategoryItem("🍲", "दाल (Dal / Lentils)"),
      CategoryItem("🥬", "तरकारी (Vegetables)"),
      CategoryItem("🛒", "किराना सामान (Groceries)"),
      CategoryItem("🥛", "दूध / दही (Milk & Curd)"),
      CategoryItem("🍗", "माछामासु (Meat & Fish)"),
      CategoryItem("🔥", "ग्यास (Gas)"),
      CategoryItem("💧", "पिउने पानी (Water)"),
      CategoryItem("☕", "खाजा / नास्ता (Snacks)"),
    ),
    "२. कोठा र मासिक बिल" to listOf(
      CategoryItem("🏠", "कोठा भाडा (Room Rent)"),
      CategoryItem("💡", "बिजुली बिल (Electricity Bill)"),
      CategoryItem("📶", "वाइफाइ / नेट (WiFi / Internet)"),
      CategoryItem("📱", "मोबाइल रिचार्ज (Mobile Recharge)"),
      CategoryItem("🧼", "साबुन / सरसफाइ (Soap & Cleaning)"),
      CategoryItem("🛋️", "घरायसी सामान (Household Items)"),
    ),
    "३. गाडी, पेट्रोल र मर्मत" to listOf(
      CategoryItem("⛽", "पेट्रोल (Petrol)"),
      CategoryItem("🚌", "गाडी भाडा (Bus / Auto / Pathao)"),
      CategoryItem("🔧", "मर्मत / सर्भिसिङ (Repair & Servicing)"),
    ),
    "४. उपचार, पढाइ र सपिङ" to listOf(
      CategoryItem("💊", "औषधि उपचार (Medical)"),
      CategoryItem("📚", "पढाइ / शिक्षा (Education)"),
      CategoryItem("🛍️", "सपिङ / लुगाकपडा (Shopping & Clothes)"),
      CategoryItem("💇", "सैलुन / पार्लर (Salon & Parlour)"),
    ),
    "५. चाडपर्व, बचत र अन्य" to listOf(
      CategoryItem("🪔", "चाडपर्व / पूजा (Festivals & Puja)"),
      CategoryItem("🎁", "उपहार / पाहुना (Gifts & Guests)"),
      CategoryItem("🎡", "घुमफिर (Outing)"),
      CategoryItem("💰", "बचत / बीमा (Savings & Insurance)"),
      CategoryItem("🏦", "ऋण / किस्ता (Loan / EMI)"),
      CategoryItem("🤝", "सापटी / उधारो (Lending / Udharo)"),
      CategoryItem("🏛️", "कर / नवीकरण (Tax & Renewal)"),
      CategoryItem("📦", "अन्य खर्च (Other Expenses)"),
    ),
  )

  val incomeGroups: Map<String, List<CategoryItem>> = linkedMapOf(
    "💼 तलब र ज्याला" to listOf(
      CategoryItem("💵", "मासिक तलब (Monthly Salary)"),
      CategoryItem("🔨", "दैनिक ज्याला (Daily Wage)"),
      CategoryItem("⏱️", "ओभरटाइम र भत्ता (Overtime & Allowance)"),
    ),
    "🏪 व्यापार र व्यवसाय" to listOf(
      CategoryItem("🛒", "पसल / रिटेलिङबाट (Shop / Retail)"),
      CategoryItem("🍽️", "होटल / खाजा घरबाट (Hotel / Cafe)"),
      CategoryItem("📦", "अनलाइन व्यापारबाट (Online Store)"),
    ),
    "🚕 सवारी र ढुवानी" to listOf(
      CategoryItem("🚌", "गाडी / अटो चलाएर (Driving)"),
      CategoryItem("🏍️", "पठाओ / इनड्राइभबाट (Ride-Sharing)"),
      CategoryItem("🚜", "गाडी भाडाबाट (Vehicle Rent)"),
    ),
    "🏠 घर / सटर बहाल" to listOf(
      CategoryItem("🛋️", "कोठा / घर बहालबाट (Room / House Rent)"),
      CategoryItem("🏬", "सटर / गोदाम बहालबाट (Shutter / Godown Rent)"),
      CategoryItem("🏞️", "जग्गा भाडाबाट (Land Lease)"),
    ),
    "🔧 सीप र सेवा" to listOf(
      CategoryItem("🛠️", "मर्मत / वर्कसपबाट (Workshop / Repair)"),
      CategoryItem("✂️", "सिलाइ / टेलरिङबाट (Tailoring)"),
      CategoryItem("💇", "सैलुन / पार्लरबाट (Salon / Parlour)"),
    ),
    "🌾 कृषि र पशुपालन" to listOf(
      CategoryItem("🥛", "डेरी / दूध बेचेर (Dairy / Milk)"),
      CategoryItem("🥬", "तरकारी / फलफूल बेचेर (Vegetables & Fruits)"),
      CategoryItem("🐔", "कुखुरा / माछा बेचेर (Poultry & Fishery)"),
    ),
    "💻 अनलाइन र डिजिटल" to listOf(
      CategoryItem("▶️", "युट्युब / फेसबुकबाट (YouTube / Social Media)"),
      CategoryItem("🌐", "अनलाइन कामबाट (Freelancing)"),
    ),
    "📚 अध्यापन र ट्युसन" to listOf(
      CategoryItem("✏️", "ट्युसन पढाएर (Tuition)"),
      CategoryItem("🎓", "कोचिङ / तालिमबाट (Coaching / Training)"),
    ),
    "✈️ रेमिट्यान्स" to listOf(
      CategoryItem("🌍", "विदेशबाट (Foreign Remittance)"),
    ),
    "🎖️ पेन्सन र भत्ता" to listOf(
      CategoryItem("👴", "मासिक पेन्सन (Monthly Pension)"),
      CategoryItem("🤝", "वृद्ध / सामाजिक भत्ता (Social Allowance)"),
    ),
  )
}
