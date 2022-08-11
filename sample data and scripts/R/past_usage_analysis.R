#//////////////////////////////////////PACKAGES

library(lubridate)


#/////////////////////////////////////VARIABLES TO BE DETERMINED BY RESEARCHER
print(getwd())
setwd("/Users/kris/Desktop/Usage App final/Data/R")

importantEvents = c(1,2, 7, 26,27)
tooLong = 60*60*2

#////////////////////////////////////CLEANING FUNCTIONS

cleaning1 <- function(data) {
  toReturn = data.frame(Unix = as.integer(),
                        App = as.character(),
                        Event = as.character())
  for (i in 1:nrow(data)){
    if (data[i,3] %in% importantEvents){
      nextRow = data.frame(Unix = data[i,1],
                           App =data[i,2],
                           Event= data[i,3])
      toReturn = rbind(toReturn, nextRow)
    }
  }
  return(toReturn)
}

cleaning2 <- function(data) {
  toDrop = c()
  for (i in 1:nrow(data)){
    if (data[i,3] == 2 & (i+1) < nrow(data) & (i- 1) > 0){
      if (data[i,1] == data[(i+1),1]){
        if (data[i,2] == data[(i + 1), 2]){
          if (data[(i-1),3] == data[(1+i),3]){
            toDrop = c(toDrop, i)
            toDrop = c(toDrop, (i+1))
          }else{
            toDrop = c(toDrop, i)
          }
        }
      }
    }  
  }
  return(data[-toDrop, ])
}

cleaning3 <- function(data){
  toDrop = c()
  for (i in 1:nrow(data)){
    if (data[i,3] != 2 & (i + 2) < nrow(data)){
      if (data[(i+1),2] == data[i, 2] & data[(i+2),2] == data[i, 2]){
        if ((data[(i+1),3] == 1 |data[(i+1),3] == 7) & (data[(i+2),3] == 1 |data[(i+2),3] == 7)){
          toDrop = c(toDrop, (i+1))
          toDrop = c(toDrop, (i+2))
        }
      }  
    }
  }
  return (data[-toDrop,])
}

cleaning4 <- function(data, tooLong){
  toDrop = c()
  for (i in 1:nrow(data)){
    if ((data[i,3] == 2 & data[i,4] == 0 ) | data[i,4] > tooLong ) {
      toDrop = c(toDrop, i)
    }
  }
  return (data[-toDrop,])
}

cleaning <- function(data, printChanges){
  data[,1] = data[,1]/1000
  data[,1] = as.integer(as.character(data[,1]))
  
  if (printChanges){
    print("intially the amount of rows in the dataframe was: ")
    print(nrow(data))
  }
  
  data = cleaning1(data)
  
  if (printChanges){
    print("after cleaning stage1 the amount of rows in the dataframe was: ")
    print(nrow(data))
  }
  
  data = cleaning2(data)
  
  if (printChanges){
    print("after cleaning stage2 the amount of rows in the dataframe was: ")
    print(nrow(data))
  }
  
  data = cleaning3(data)
  
  if (printChanges){
    print("after cleaning stage3 the amount of rows in the dataframe was: ")
    print(nrow(data))
  }
  
  duration = c()
  for (i in 1:nrow(data)){
    if (i < nrow(data)){
      duration = c(duration, data[(i+1),1] - data[i,1])
    }
  }
  duration = c(duration,0)
  data[,4] = duration
  
  data = cleaning4(data, tooLong)
  
  if (printChanges){
    print("after cleaning stage3 the amount of rows in the dataframe was: ")
    print(nrow(data))
  }
  
  return(data)
}

#///////////////////////////////////////////////////////CORE CODE

df <- read.csv(file="past usage.csv", header=TRUE, sep=",")
df = cleaning(df, TRUE)
colnames(df) = c("Unix", "App", "Event", "Duration")


time <- df[,1]
time <- as.POSIXct(time, origin = "1970-01-01")

tperiod <- diff(time, lag=1, differences = 1)
df <- data.frame(df, data.frame(time), data.frame(substr(time, 9,10)))

colnames(df) = c("Unix", "App", "Event", "Duration", "time", "day")

#ESTABLISHING SCREEN TIME

screenUsage = c()

for (day in unique(df$day)){
  dayDF = df[df$day == day,]
  dayDF = dayDF[dayDF$Event != 2,]
  screenUsage = c(screenUsage, sum(dayDF$Duration)/60/60)
}

print(screenUsage)

#ESTABLISHING TOP USED APPS

timeAppsUsed = data.frame(App = as.character(),
                          Duration = as.integer())
for (app in unique(df$App)){
  appDF = df[df$App == app,]
  appDF = appDF[appDF$Event != 2,]
  timeAppsUsed = rbind(timeAppsUsed, data.frame(App = app, Duration = sum(appDF$Duration)/60/60 ))
}

timeAppsUsed <- timeAppsUsed[order(-timeAppsUsed$Duration),]
print(timeAppsUsed)

#//////////////////////////////////////////INTEGRATING THE CONTEXTUAL DATA

context = read.csv("context.csv")
context = context[,1:3]
colnames(context) = c("App", "Permission", "Response")

context = context[context$Permission == "android.permission.ACCESS_FINE_LOCATION",]
context = context[context$Response == "true",]

locationSensingApps = unique(context$App)
locationSensingApps = droplevels(locationSensingApps)
print(locationSensingApps)

locationDF = df[df$App %in% locationSensingApps,]
locationDF = locationDF[locationDF$Event!=2,]
print("duration of usage across the two days for location sensing apps")
print(sum(locationDF$Duration)/60/60)

