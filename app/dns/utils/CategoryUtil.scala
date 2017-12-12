package dns.utils

object CategoryUtil {
  private val CATEGORY_STRING = """
    Arts and Entertainment
    Arts and Entertainment > Animation and Comics
    Arts and Entertainment > Architecture
    Arts and Entertainment > Awards
    Arts and Entertainment > Celebrities and Entertainment News
    Arts and Entertainment > Fashion and Modeling
    Arts and Entertainment > Humor
    Arts and Entertainment > Movies
    Arts and Entertainment > Music and Audio
    Arts and Entertainment > Performing Arts
    Arts and Entertainment > Photography
    Arts and Entertainment > TV and Video
    Arts and Entertainment > Visual Arts and Design
    Autos and Vehicles
    Autos and Vehicles > Automotive Industry
    Autos and Vehicles > Automotive News
    Autos and Vehicles > Aviation
    Autos and Vehicles > Boating
    Autos and Vehicles > Car Buying
    Autos and Vehicles > Car Rentals
    Autos and Vehicles > Makes and Models
    Autos and Vehicles > Motorcycles
    Autos and Vehicles > Motorsports
    Autos and Vehicles > Trains and Railroads
    Beauty and Fitness
    Beauty and Fitness > Beauty
    Beauty and Fitness > Bodyart
    Beauty and Fitness > Cosmetics
    Beauty and Fitness > Fitness
    Beauty and Fitness > Hair
    Beauty and Fitness > Skin Care
    Beauty and Fitness > Weight Loss
    Books and Literature
    Books and Literature > Book Retailers
    Books and Literature > E Books
    Books and Literature > Folklore
    Books and Literature > Guides and Reviews
    Business and Industry
    Business and Industry > Aerospace and Defence
    Business and Industry > Agriculture and Forestry
    Business and Industry > Associations
    Business and Industry > Biotechnology and Pharmaceuticals
    Business and Industry > Business Services
    Business and Industry > Chemicals
    Business and Industry > Construction and Maintenance
    Business and Industry > Customer Service
    Business and Industry > E Commerce
    Business and Industry > Energy
    Business and Industry > Industrial Goods and Services
    #Business and Industry > Marketing and Advertising
    Business and Industry > Metals and Mining
    Business and Industry > Publishing and Printing
    Business and Industry > Real Estate
    Business and Industry > Textiles and Nonwovens
    Business and Industry > Transportation and Logistics
    Business and Industry > Wholesale Trade
    Career and Education
    Career and Education > Business Training
    Career and Education > Education
    Career and Education > Human Resources
    Career and Education > Jobs and Employment
    Career and Education > Universities and Colleges
    Computer and Electronics
    Computer and Electronics > Computer Hardware
    Computer and Electronics > Computer Security
    Computer and Electronics > Consumer Electronics
    Computer and Electronics > Graphics and Multimedia Tools
    Computer and Electronics > Networking
    Computer and Electronics > Programming
    Computer and Electronics > Software
    Finance
    Finance > Accounting
    Finance > Banking
    Finance > Credit, Loans and Mortgages
    Finance > Financial Management
    Finance > Grants and Scholarships
    Finance > Insurance
    Finance > Investing
    Food and Drink
    Food and Drink > Beverages
    Food and Drink > Catering
    Food and Drink > Cooking and Recipes
    Food and Drink > Food and Grocery Retailers
    Food and Drink > Restaurants and Delivery
    Food and Drink > Vegetarian and Vegan
    Gambling
    Gambling > Bingo
    Gambling > Casinos
    Gambling > Lottery
    Gambling > Poker
    Gambling > Regulation and Organizations
    Gambling > Sports
    Games
    Games > Board and Card Games
    Games > Miniatures
    Games > Online
    Games > Puzzles and Brainteasers
    Games > Roleplaying
    Games > Video Games
    Health
    Health > Addictions
    Health > Alternative and Natural Medicine
    Health > Child Health
    Health > Conditions and Diseases
    Health > Dentistry
    Health > Education and Resources
    Health > Healthcare Industry
    Health > Medicine
    Health > Mens Health
    Health > Mental Health
    Health > Nutrition
    Health > Pharmacy
    Health > Products and Shopping
    Health > Public Health and Safety
    Health > Reproductive Health
    Health > Senior Health
    Health > Womens Health
    Home and Garden
    Home and Garden > Gardening
    Home and Garden > Home Improvement
    Home and Garden > Interior Decor
    Home and Garden > Moving and Relocating
    Home and Garden > Nursery and Playroom
    Internet and Telecom
    Internet and Telecom > Ad Network
    Internet and Telecom > Chats and Forums
    Internet and Telecom > Domain Names and Register
    Internet and Telecom > Email
    Internet and Telecom > File Sharing
    Internet and Telecom > Mobile Developers
    #Internet and Telecom > Online Marketing
    Internet and Telecom > Search Engine
    Internet and Telecom > Social Network
    Internet and Telecom > Telecommunications
    Internet and Telecom > Web Design
    Internet and Telecom > Web Hosting
    Law and Government
    Law and Government > Government
    Law and Government > Immigration and Visas
    Law and Government > Law
    Law and Government > Military and Defense
    News and Media
    News and Media > Business News
    News and Media > College and University Press
    News and Media > Magazines and E Zines
    News and Media > Newspapers
    News and Media > Sports News
    News and Media > Technology News
    News and Media > Weather
    People and Society
    People and Society > Crime and Prosecution
    People and Society > Death
    People and Society > Disabled and Special Needs
    People and Society > Gay, Lesbian, and Bisexual
    People and Society > Genealogy
    People and Society > History
    People and Society > Holidays
    People and Society > Philanthropy
    People and Society > Philosophy
    People and Society > Relationships and Dating
    People and Society > Religion and Spirituality
    People and Society > Womens Interests
    Pets and Animals
    Pets and Animals > Animal Products and Service
    Pets and Animals > Birds
    Pets and Animals > Fish and Aquaria
    Pets and Animals > Horses
    Pets and Animals > Pets
    Recreation and Hobbies
    Recreation and Hobbies > Antiques
    Recreation and Hobbies > Camps
    Recreation and Hobbies > Climbing
    Recreation and Hobbies > Collecting
    Recreation and Hobbies > Crafts
    Recreation and Hobbies > Models
    Recreation and Hobbies > Nudism
    Recreation and Hobbies > Outdoors
    Recreation and Hobbies > Scouting
    Recreation and Hobbies > Theme Parks
    Recreation and Hobbies > Tobacco
    Recreation and Hobbies > Weapons
    Reference
    Reference > Archives
    Reference > Ask an Expert
    Reference > Dictionaries and Encyclopedias
    Reference > Directories
    Reference > Libraries and Museums
    Reference > Maps
    Science
    Science > Agriculture
    Science > Astronomy
    Science > Biology
    Science > Chemistry
    Science > Earth Sciences
    Science > Educational Resources
    Science > Engineering and Technology
    Science > Environment
    Science > Instruments and Supplies
    Science > Math
    Science > Physics
    Science > Social Sciences
    Shopping
    Shopping > Antiques and Collectibles
    Shopping > Auctions
    Shopping > Children
    Shopping > Classifieds
    Shopping > Clothing
    Shopping > Consumer Electronics
    Shopping > Coupons
    Shopping > Ethnic and Regional
    Shopping > Flowers
    Shopping > Furniture
    Shopping > General Merchandise
    Shopping > Gifts
    Shopping > Home and Garden
    Shopping > Jewelry
    Shopping > Music
    Shopping > Office Products
    Shopping > Publications
    Shopping > Sports
    Shopping > Weddings
    Sports
    Sports > Baseball
    Sports > Basketball
    Sports > Boxing
    Sports > Cycling and Biking
    Sports > Equestrian
    Sports > Extreme Sports
    Sports > Fantasy Sports
    Sports > Fishing
    Sports > Football
    Sports > Golf
    Sports > Martial Arts
    Sports > Rugby
    Sports > Running
    Sports > Soccer
    Sports > Tennis
    Sports > Volleyball
    Sports > Water Sports
    Sports > Winter Sports
    Travel
    Travel > Accommodation and Hotels
    Travel > Airlines and Airports
    Travel > Roads and Highways
    Travel > Tourism
    Adult
    Marketing and Advertising
    Blog
    """

  private val CATEGORY = CATEGORY_STRING.split("\n")
    .filter(x => x != "" && !x.startsWith("#"))
    .map(x => x.split(">"))
    .map(x => {
      if (x.length == 2) x(0).trim() -> x(1).trim() else x(0).trim() -> x(0).trim()
     })
    .groupBy(x => x._1)
    .mapValues(x => x.map(y => y._2))


  def main(args: Array[String]) {
    println(CATEGORY.size)
    println(CATEGORY.map(x => x._2.size).sum)
  }
}