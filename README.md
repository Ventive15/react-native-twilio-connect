
# A React Native wrapper module for the [Twilio](https://www.twilio.com) mobile SDK

## Getting started

`$ npm install react-native-twilio-connect --save` OR `$ yarn add react-native-twilio-connect --save`

### Mostly automatic installation

`$ react-native link react-native-twilio-connect`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-twilio-connect` and add `RNTwilio.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNTwilio.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNTwilioPackage;` to the imports at the top of the file
  - Add `new RNTwilioPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-twilio-connect'
  	project(':react-native-twilio-connect').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-twilio-connect/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      implementation project(':react-native-twilio-connect')
  	```


## Usage
```javascript
import RNTwilio from 'react-native-twilio-connect';

// TODO: What to do with the module?
RNTwilio;
```
  
