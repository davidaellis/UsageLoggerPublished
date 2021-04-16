
library("plyr")
library("dplyr")
library("magrittr")
library("ggplot2")
library("ggExtra")
library("ggthemes")
#past is the past.csv one from github -- already ran through the cleaning script...
#setwd() #set to where past.csv is
past = read.csv("past.csv") #read in csv


#here we are taking the past df and looking at the average duration of usage by app (in seconds)
past_mean = past %>%
  group_by(App) %>%
  summarise(mean_duration = mean(Duration)) 

#here we are taking the past df and looking at the total duration of usage by app (in minutes)
past_total = past %>%
  group_by(App) %>%
  tally(Duration)

past_total = mutate(past_total, minutes = n/60)

#plotting example average durations
plotdata = subset(past_mean, App == "TikTok" | App == "YouTube" |
                    App == "WhatsApp" | App == "Gmail" | App == "Messages" | App == "WeChat" | App == "Chrome" | App == "Camera"| App == "Audible")

#then taking this and making a simple bar plot
plotdata %>%
  ggplot(aes(reorder(App, -mean_duration), mean_duration)) +
  geom_bar(stat = "identity", fill="steelblue") +
  xlab("App") +
  ylab("Average Duration of Each Usage (seconds)") +
  theme_classic(base_size = 18)

#plotting example total durations
plotdata = subset(past_total, App == "TikTok" | App == "YouTube" |
                    App == "WhatsApp" | App == "Gmail" | App == "Messages" | App == "WeChat" | App == "Chrome" | App == "Camera"| App == "Audible")

#then taking this and making a simple bar plot (Figure 2)
plotdata %>%
  ggplot(aes(reorder(App, -minutes), minutes)) +
  geom_bar(stat = "identity", fill="steelblue") +
  xlab("App") +
  ylab("Total Usage Time (minutes)") +
  theme_classic(base_size = 18)
 
