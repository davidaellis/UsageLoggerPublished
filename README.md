# Usage Logger

Usage logger is an Android app that records or extracts data about how a person uses their smartphone. Primarily developed to assist with psychological research and behavioural science more generally, the app can be customised to perform a number of different functions. This includes the ability to extract historical usage data from a smartphones internal memory.  

Published-paper: 

Geyer, K., Ellis, D.A., Shaw, H. and Davidson, B. I. (in press) Open-source smartphone app and tools for measuring, quantifying, and visualizing technology use. Behavior Research Methods [LINK](https://doi.org/10.3758/s13428-021-01585-7) [post-print](https://psyarxiv.com/eqhfa)

Download a working version from the [Google play store](https://play.google.com/store/apps/details?id=geyerk.sensorlab.suselogger). 

A Terms of Service and Privacy Policy is available [here](https://psychsensorlab.com/privacy-agreement-for-apps/)

Sample data and supplementary R-code for analysis are available in the folder 'Data' above. Note: the password to decrypt raw unprocessed data is L8l%Y%!b-4hW

Customise how the app works [here](https://usageloggersetup.netlify.app/ )

Decrypt data [here](https://usageloggerdecryptwebsite.netlify.app/)

An extensive walkthrough can be found [here](https://u-log-walk.netlify.app/)

## Customisation

This is  discussed in detail as part of the academic paper above (see above link). Contextual data includes: 

* Installed apps
* Permissions apps request
* Permissions accepted/rejected by participants

Continuous logging can record a number of different behaviours following installation including: 

* Screen on/off events
* What apps are used and when
* When apps send notifications
* When apps are added or removed (installed or uninstalled)

Past usage includes data prior to installation including:

* Screen on/off events
* What apps are used and when

## FAQ

1. Why do I need a decrypting website

You don’t need the decrypting website. You can open the encrypted pdf files with the correct password. However, we have supplied a website that automatically decrypts and converts pdf files into csv files. PDF files are used as this allows participants to view their own data on their device at any time. 

2. You've previously talked about doze mode, is this still a problem?

In a previous [app](https://github.com/kris-geyer/pegLog) we flagged issues with 'doze mode'. This is where the Android operating system attempt to restrict activity on a device after certain periods of inactivity, which can adversly impact background data collection processes. However, this is not an issue for Usage Logger because all activity recorded occurs while the phone is active. 

## Bugs/known issues

Report bugs or functionality issues to k.geyer2@lancaster.ac.uk

## Lab website

You can read more about our work [here](https://psychsensorlab.com/)
