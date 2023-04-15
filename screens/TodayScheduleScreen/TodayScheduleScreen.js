import React, {useState, useEffect} from 'react';
import { View, Text, Image, StyleSheet, useWindowDimensions, ScrollView, TextInput } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import {useForm, Controller} from 'react-hook-form';
import Audio1 from '../../assets/images/audio1.png';
import Audio2 from '../../assets/images/audio2.png';
import Plus from '../../assets/images/plus.png';
import Minus from '../../assets/images/minus.png';
import useSpeechToText, { ResultType } from '../StudentScreen/Hooks';
import { Audio } from 'expo-av';

const TodayScheduleScreen = () => {
  const navigation = useNavigation();
  const [keyword, setKeyword] = useState("");
  const [recording, setRecording] = React.useState();
  const [soundUrl, setSoundUrl] = useState("");

  const handleKeywordChange = (value) => {
    setKeyword(value);
  };

  const handleSoundUrlChange = (value) => {
    setSoundUrl(value);
  };

  const handleBack = () => {
    console.log(keyword)
    console.log(results)
    navigation.navigate('ParentScreen', { keyword: keyword, soundUrl: soundUrl, answer: (results).map((result) => (result.transcript))});
  }

  const handlePlus = () => {
    navigation.navigate('TodayScheduleScreenTwo')
  }

  const {
    error,
    interimResult,
    isRecording,
    results,
    startSpeechToText,
    stopSpeechToText
  } = useSpeechToText({
    continuous: true,
    crossBrowser: true,
    timeout: 3000,
    googleApiKey: process.env.REACT_APP_API_KEY,
    speechRecognitionProperties: { interimResults: true },
    useLegacyResults: false
  });

  async function startRecording() {
    try {
      console.log('Requesting permissions..');
      await Audio.requestPermissionsAsync();
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: true,
        playsInSilentModeIOS: true,
      }); 
      console.log('Starting recording..');
      const recording = new Audio.Recording();
      await recording.prepareToRecordAsync(Audio.RECORDING_OPTIONS_PRESET_HIGH_QUALITY);
      await recording.startAsync(); 
      setRecording(recording);
      console.log('Recording started');
    } catch (err) {
      console.error('Failed to start recording', err);
    }
  }
  async function stopRecording() {
    console.log('Stopping recording..');
    setRecording(undefined);
    await recording.stopAndUnloadAsync();
    const uri = recording.getURI(); 
    handleSoundUrlChange(uri);
    console.log(soundUrl);
    console.log('Recording stopped and stored at', uri);
  }
  
  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Today's Schedule</Text>
      <br/><br/><br/>
      
      <div style={{display: "inline", width: "100%"}}>
        <Text style={styles.text}>Action Item 1</Text>
        <TextInput
          placeholder="Keyword"
          style={{ paddingLeft: 20, paddingTop: 12, paddingBottom: 12, borderRadius: 10, height: 45, marginLeft: 18, fontSize: 18, width: 210, marginTop: 24, backgroundColor: 'white' }}
          onChangeText={handleKeywordChange}
          text={keyword}
        ></TextInput>
        <br/>
        <img width="90" height="90" style={{}} src={recording ? Audio2 : Audio1} onClick={recording ? stopRecording : startRecording}/>
        <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          {(results).map((result) => (
            <li key={result.timestamp}>{result.transcript}</li>
          ))}
          {interimResult && <li>{interimResult}</li>}
      </ul>
      </div>

      <br/>
      <div>
        <img width="95" height="90" src={Minus}/>
        <Text>       </Text>
        <img width="95" height="90" src={Plus} onClick={handlePlus}/>
      </div>

      <br/>
      <CustomButton text={"Save"} onPress={handleBack} type="EXTRATWO"/>
      <br/>
      <CustomButton text={"Go Back"} onPress={handleBack} type="SECONDARY"/>
      <br/><br/><br/><br/>
    </View>
  )
}

const styles = StyleSheet.create({
  root: {
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#364d59',
    height: "max",
  },
  title: {
    fontSize: 36,
    fontWeight: 'bold',
    color: 'white',
  },
  text: {
    fontSize: 20,
    color: 'white',
  },
  text2: {
    marginTop: "10px",
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
});

export default TodayScheduleScreen;