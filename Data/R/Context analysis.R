#//////////////////////////////////////PACKAGES

library(lubridate)


#/////////////////////////////////////VARIABLES TO BE DETERMINED BY RESEARCHER
print(getwd())
setwd("/Users/kris/Desktop/Usage App final/Data/R")

#///////////////////////////////////////////////////////CORE CODE

df <- read.csv(file="context.csv", header=TRUE, sep=",")
df = df[,c(1,2,3)]

colnames(df) = c("App", "Permission", "response")


#////////////////////////////////////////////////////////IDENTIFY NUMBER OF PERMISSIONS PER APP

appPerm = data.frame(App = as.character(), Permissions = as.integer())

for (app in unique(df$App)){
  appPerm = rbind(appPerm, data.frame(App = app, Permissions = nrow(df[df$App == app,]) ))
}

print(appPerm)


#////////////////////////////////////////////////////////IDENTIFYING NUMBER OF PERMISSIONS REQUESTED, APPROVED AND NOT APPROVED

appPermsResponse = data.frame(App = as.character(), PermsApproved = as.integer(), PermsRejected = as.integer())

for (app in unique(df$App)){
  appPermsResponse = rbind(appPermsResponse, data.frame(App = app,
                                                        PermsApproved = nrow(df[df$App == app & df$response == "true",]),
                                                        PermsRejected = nrow(df[df$App == app & df$response == "false",])))
}

print(appPermsResponse)

#/////////////////////////////////////////////////////IDENTIFYING WHICH APPS ARE ACCEPTED OR REJECTED FOR ACCESSING PARTICULAR PERMISSIONS

permDF = data.frame(Permission = as.character(), Rejected = as.integer(), rApps = as.character(), Approved = as.integer(), aApps = as.character())

for (permission in unique(df$Permission)){
  perms = df[df$Permission == permission,]
  
  rejects = perms[perms$response == "false",]
  rApps = ""
  for (reject in rejects$App){
    rApps = paste(paste(rApps, ", ", sep = "") , reject, sep = "")
  }
  
  
  approves = perms[perms$response == "true",]
  aApps = ""
  for (approved in approves$App){
    aApps = paste(paste(aApps, ", ", sep = "") , approved, sep = "")
  }
  
  permDF = rbind(permDF,
                 data.frame(
                 Permission = permission,
                 Rejected = nrow(rejects),
                 rApps = rApps, 
                 Approved = nrow(approves),
                 aApps = aApps
                 ))
}

print(permDF)
