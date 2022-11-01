import React, {useState, useEffect} from 'react';
import { View, Text, Image, StyleSheet, useWindowDimensions, ScrollView, TextInput } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import {useForm, Controller} from 'react-hook-form';
import Audio1 from '../../assets/images/audio1.png';
import Plus from '../../assets/images/plus.png';
import Minus from '../../assets/images/minus.png';
import useSpeechToText, { ResultType } from '../StudentScreen/Hooks';

const TodayScheduleScreen = () => {
  const navigation = useNavigation();
  const {control, watch} = useForm();
  const keyword = watch('keyword');

  const handleBack = () => {
    navigation.navigate('ParentScreen')
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

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Today's Schedule</Text>
      <br/><br/><br/>
      
      <div style={{display: "inline", width: "100%"}}>
        <Text style={styles.text}>Action Item 1</Text>
        <input placeholder="Keyword" style={{height: 35, marginLeft: 18, fontSize: 18, width: 175, marginTop: 24}}></input>
        <img width="100" height="90" style={{}} src={Audio1} onClick={isRecording ? stopSpeechToText : startSpeechToText}/>
        <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          {(results).map((result) => (
            <li key={result.timestamp}>{result.transcript}</li>
          ))}
          {interimResult && <li>{interimResult}</li>}
      </ul>
      </div>

      <br/><br/>
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