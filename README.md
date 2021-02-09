# Usage Logger

Usage logger is an Android app that records or extracts data about how a person uses their smartphonephone. Primarily developed to assist with psychological research and behavioural science more generally, the app can be customised to perform a number of different functions. This includes an ability to extract historical data about previous (retrospective) usage form the previous 5 days. 

Pre-Print:

Geyer, K., Ellis, D. A., Shaw, H. and Davidson, B. I. (2020). Open source smartphone apps and tools for measuring, quantifying, and visualizing technology use. PsyArXiv, [LINK](https://psyarxiv.com/eqhfa)

Download a working version from the [Google play store](https://play.google.com/store/apps/details?id=geyerk.sensorlab.suselogger). 

A Terms of Service and Privacy Policy is available [here](https://usage-logger-privacy.netlify.com/)

Sample data and supplementary R-code for analysis are available in the folder 'Data' above. Note: the password to decrypt raw unprocessed data is L8l%Y%!b-4hW

Decrypt data [here](https://usage-logger-decrypt.netlify.com/ )

Customise how the app works [here](https://usage-logger-custom.netlify.com/ )

An extensive walkthrough can be found [here](https://u-log-walk.netlify.app/)

## Customisation

This is also discussed in the customisation website (see above link). Prospective logging can record a number of different behaviours following installation including: 

* When the screen is on/off
* When/what apps are used
* When notifications are sent & which apps send these
* When apps are added (installed) or removed (uninstalled)

Retrospective extraction includes data prior to installation including:

* When the screen is on/off
* When apps are opened

Contextual data includes: 

* What apps are installed
* What permissions apps request
* What permissions requests are accepted/rejected by participants

## FAQ

1. Why do I need a decrypting website

You don’t need the decrypting website. You can open the encrypted pdf files with the correct password. However, we have supplied a website that automatically decrypts and converts pdf files into csv files. PDF files are used as this allows participants to view their own data on their device at any time. 

2. You've previously talked about doze mode, is this still a problem?

In a previous [app](https://github.com/kris-geyer/pegLog) we flagged issues with 'doze mode'. This is where the Android operating system attempt to restrict activity on a device after certain periods of inactivity, which can adversly impact background data collection processes. However, this is not an issue for Usage Logger because all activity recorded occurs while the phone is active. 

## bugs/known issues

Report bugs or functionality issues to k.geyer2@lancaster.ac.uk

## lab website

You can read more about our work [here](www.psychsensorlab.com)
