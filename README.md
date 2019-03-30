# Shopping_Apps


HIGLIGHTS OF MAJOR TECHNOLOGIES USED:
1.	A custom-built version of Google’s Volley library allowing it to work with Flipkart’s servers.
2.	REST-based requests to Amazon AWS servers.
3.	Firebase Database, Authentication and Push Notifications.
4.	GSON Library
5.	Chrome custom tabs

If you are an employer/recuiter looking for hiring an android developer intern, please contact me at satyam.cse17@nituk.ac.in


APP DESCRIPTION: 
This app allows users to quickly access their favourite shopping sites without having the hassle of installing separate applications. These sites open in chrome custom tabs, thus allowing users to save their passwords with Google Smart Lock. 
They can also use a unified search which connects with Flipkart Affiliate API and Amazon product advertising API simultaneously to find products and their best prices from both marketplaces based on search keywords.

A live working version of this app is available from here: https://play.google.com/store/apps/developer?id=Satyam+Kumar

SECURITY OF API KEYS: API Keys are retrieved during runtime from a Firebase database which is configured to connect with apps having fingerprint same as that of my Google Play Account’s app signing. Since it is impossible for anyone (including me!) to obtain those signing keys, reverse engineering of APK’s to obtain secret API keys of my Flipkart/Amazon account is not possible. However, the only possible way for a hacker to get access to secret API keys is by obtaining access to the Firebase account with which the app connects.

About Flipkart Affiliate API: When making an HTTPS request, developers are required to curl their API Token and Affiliate tag inside headers, for which this app uses a customised version of Google’s Volley library (Please see CustomRequest class in SearchActivity). This project uses Google’s GSON Library for parsing JSON response received from Flipkart’s servers.

About Amazon Affiliate API: A REST request to AWS contains access key and associate tag as one of the query parameters. All the query parameters are then hashed with a secret key which is then used as a value of the Signature parameter. This signature parameter is added at last to request URL and is used by Amazon’s servers to verify the authenticity of originating requests. In simple terms, it prevents any hacker from changing parameters when a request is being sent over the network. Amazon’s servers then provide a response in form of XML which is then parsed to extract useful information and displaying it to users. 

Please note that GoogleServices.json file has been removed from this project and you might want to add one to your project before building this project in Android Studio.
