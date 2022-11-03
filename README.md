#Project name: API Proxy

Team members and contributions: Zoe Zimmermann (zzimmerm), Justin Cheng (jcheng59)

Total estimated time it took to complete project: 10 hours

A link to your repo: https://github.com/cs0320-f2022/sprint-2-jcheng59-zzimmerm

###Explain the relationships between classes/interfaces: 
The Server class holds the main functionality of our API server. By constructing a server and adding datasources,
it calls Spark and sets everything up on the network.

CSVGetHandler, CSVLoadHandler, and WeatherHandler are the datasources we provided to Server for this project. They load a CSV, retrieve the contents of a CSV, and accessing the temperature from the National Weather Service. 
CSVLoadHandler uploads the uploaded CSV data into a Storage object. CSVGetHandler can then retrieve that Storage object.

We also created BadEndpointHandler to handle invalid requests to our API (i.e. non-existent endpoints).

###Discuss any specific data structures you used, why you created it, and other high level explanations.
We created the HandlerWithStorage interface to ensure that datasources with support for storage are passed to the addLoadGetDatasource.
CSVGetHandler and CSVLoadHandler both implement this interface.


###Tests -- Explain the testing suites that you implemented for your program and how each test ensures that a part of the program works.
Within the testServer test file, we test the functionality of CSVGetHandler, CSVLoadHandler, and WeatherHandler.
For WeatherHandler, we test that we accurately access the NWS weather data for a given location. 
We also test that our errors (no coordinates inputted, coordinates outside of the US, and coordinates not inputted as floats) are handled correctly.
For CSVLoadHandler, we test that a CSV can be correctly loaded. We also test that our errors (issues reading the file, invalid filepath, or no filepath provided) are handled correctly.
For CSVGetHandler, we test that the contents of a loaded CSV can be retrieved. We also test that it displays an error when no file has been loaded. 

We also did unit testing for WeatherHandler in TestWeather, for each method of WeatherHandler.

Our tests cover each discrete function of our programs individually (unit testing). Each time we run our tests, it starts our server and uses our API (integration testing).

###How to run the tests you wrote/were provided:
Open your terminal, navigate to the project directory, type `mvn run` and press enter!

###How to build and run your program:
* Run Main, which starts a server on your computer
* Access one of the following URLs through a browser: 
  * localhost:3232/weather?lat=[add latitude here]&lon=[add longitude here]
    * This will show the current temperature at the coordinates entered
  * localhost:3232/loadCSV?filepath=[add filepath here]
    * This will load a CSV file
    * The filepath can either be absolute (i.e. from your root directory) or
    relative (i.e. from the project directory)
  * localhost:3232/getCSV?
    * This will return the contents of the loaded CSV