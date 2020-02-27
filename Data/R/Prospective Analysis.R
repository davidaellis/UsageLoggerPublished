#//////////////////////////////////////PACKAGES

library(lubridate)


#/////////////////////////////////////VARIABLES TO BE DETERMINED BY RESEARCHER
print(getwd())
setwd("/Users/kris/Desktop/Usage App final/Data/R")

#////////////////////////////////////CORE CODE

df = read.csv("prospective.csv")
df = df[,1:2]
colnames(df) = c("Unix", "Event")
print(df, digits = 16)

#/////////////////////////////////////ESTABLISHING VARIABLES

#establishing duration
duration = c()
for (row in 1:nrow(df)-1 ){
  duration = c(duration, df$Unix[row+1] - df$Unix[row])
}
duration = c(duration, 0)

df$duration = duration


#Establsihing screen usage

establishScreenUsage = function (data) {
  startOfUsage = 0
  screenUsage = 0
  screenState = "none"
  
  for (row in 1:nrow(data)){
    if (data$Event[row] == "screen on" & screenState != "screen on"){
      startOfUsage = data$Unix[row]
      screenState = "screen on"
    }
    if (data$Event[row] == "screen off" & screenState == "screen on"){ 
      screenUsage = screenUsage +  (data$Unix[row] - startOfUsage)
      screenState = "screen off"
    }
  }
  return (screenUsage)
}


screenUsage = establishScreenUsage(df)
print(screenUsage)
print(((screenUsage/1000)/60)/60)

#now returning screen usage per day

df$Date = substr(as.Date(as.POSIXct(df$Unix/1000, origin="1970-01-01")),9,10) 

dayScreenUsage = data.frame(Day = as.character(), Usage = as.integer())

for (day in unique(df$Date)){
  dayDF= df[df$Date == day,]
  dayScreenUsage = rbind(dayScreenUsage, data.frame(Day = day, Usage= establishScreenUsage(dayDF)/1000/60/60))  
}

print(dayScreenUsage)


#Establishing most used apps

Apps = c()
AppUsage = data.frame(App = as.character(), Duration = as.integer())

for (event in unique(df$Event)){
  if (substr(event, 1, 3) == "App"){
    Apps = c(Apps, event)
  }
}

for (app in Apps){
  AppUsage = rbind(AppUsage, data.frame(App = app,
                                        Duration = sum(df[df$Event == app,]$duration)/1000/60/60))
}

AppUsage = AppUsage[order(-AppUsage$Duration),]

print(AppUsage)


#///////////////////////////////////////INTEGRATING THE CONTEXTUAL DATA

context = read.csv("context.csv")
context = context[,1:3]
colnames(context) = c("App", "Permission", "Response")

context$App = as.character(context$App)

context = context[context$Permission == "android.permission.ACCESS_FINE_LOCATION",]
context = context[context$Response == "true",]

locationSensingApps = unique(context$App)

print(locationSensingApps)

for (element in 1:length(locationSensingApps)){
  locationSensingApps[element] = paste("App:", sep = " ", locationSensingApps[element])
}

print(locationSensingApps)

locationDF = df[df$Event %in% locationSensingApps,]
print(sum(locationDF$duration)/1000/60/60)


