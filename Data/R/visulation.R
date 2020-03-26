library("plyr")
library("dplyr")
library("magrittr")
library("ggplot2")
library("ggExtra")
library("ggthemes")
#past is the past.csv one from github -- run the cleaning script... and check variable names match
setwd() #set to where past.csv is
past = read_csv(past.csv) #read in csv

#here we are taking the past df and looking at the average duration of usage by app
past_mean = past %>%
  group_by(App) %>%
  summarise(mean_duration = mean(Duration)) 

plotdata = subset(past_mean, App == "TikTok" | App == "YouTube" |
                    App == "WhatsApp" | App == "Gmail" | App == "Messages" | App == "WeChat")
#then taking this and making a simple bar plot
plotdata %>%
  ggplot(aes(reorder(App, -mean_duration), mean_duration)) +
  geom_bar(stat = "identity", fill="steelblue") +
  xlab("App") +
  ylab("Average Duration of Usage (seconds)") +
  theme_classic()