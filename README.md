# Usage Logger 2

## Introduction
Usage Logger 2 is an Android app that records or extracts data about how a person uses their smartphone. Primarily developed to assist with psychological research and behavioural science more generally, the app can be customised to perform a number of different functions.  This includes the ability to extract historical data about previous (retrospective) usage from the previous 5 days.

### Publications

Geyer, K., Ellis, D.A., Shaw, H. and Davidson, B. I. (2022) Open-source smartphone app and tools for measuring, quantifying, and visualizing technology use. <em>Behavior Research Methods, 54</em>(1), 1-12, doi: [10.3758/s13428-021-01585-7](https://doi.org/10.3758/s13428-021-01585-7)

Geyer, K., Ellis, D. A., Shaw, H., & Davidson, B. I. (2020, July 21). *Open source smartphone app and tools for measuring, quantifying, and visualizing technology use.* PsyArXiv. doi: [10.31234/osf.io/eqhfa](https://doi.org/10.31234/osf.io/eqhfa)

### Other resources
- Download a working version from [Google Play Store](https://play.google.com/store/apps/details?id=geyerk.sensorlab.suselogger).
- Customise how the app works [here](https://usageloggersetup.netlify.app/)
- Decrypt data [here](https://usageloggerdecryptwebsite.netlify.app/)
- An extensive walkthrough can be found [here](https://u-log-walk.netlify.app/)
- A Privacy Policy is available [here](https://usagelogger2.netlify.app/privacy/)
- Sample data and scripts (in R and Python) are supplied. See 'sample data and scripts' folder above.

## Customisation

This is discussed in detail as part of the academic paper above (see above link). The customisation website can be found [here](https://usageloggersetup.netlify.app/).

**Contextual data**

*Types of data to be recorded:*

- Installed apps
- Permissions apps request
- Permissions accepted/rejected by participants

**Continuous logging** 

*Types of behaviours to be recorded:*

- Screen on/off events
- What apps are used and when
- When apps send notifications
- When apps are added or removed (installed or uninstalled)

**Past usage** 

*Logs prior to app installation (max. 5 days), including:*

- Screen on/off events
- What apps are used and when

## FAQ

### Do I need to use the decryption website?
You don’t need to use the decrypting website, if you open the encrypted PDF files with the correct password. However, we have created a website that will automatically decrypt and convert PDF files into CSV files for your convenience. PDF files are used as this allows participants to view their own data on their device at any time. 

### I've head that doze mode can be a problem. Is this still relevant?
In a [previous app](https://github.com/kris-geyer/pegLog) we flagged issues with 'doze mode'. This is where the Android operating system attempts to restrict activity on a device after certain periods of inactivity, which can adversely impact background data collection processes. However, this is not an issue for Usage Logger 2 because all recorded activity occurs while the phone is active. 

## Contact / Lab
If you have any problems or specific questions, you are welcome to contact us at <a href="mailto:h.shaw5@lancaster.ac.uk">h.shaw5@lancaster.ac.uk</a>. You can read more about our work on our [lab website](https://psychsensorlab.com/).