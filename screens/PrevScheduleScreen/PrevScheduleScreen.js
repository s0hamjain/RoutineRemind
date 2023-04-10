import React, {useState, useEffect} from 'react';
import { View, Text, Image, StyleSheet, useWindowDimensions, ScrollView, TextInput } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import {useForm, Controller} from 'react-hook-form';
import Audio1 from '../../assets/images/playaudio.png';
import useSpeechToText, { ResultType } from '../StudentScreen/Hooks';

import Basketball from '../../assets/images/Basketball.m4a'
import Piano from '../../assets/images/Piano.m4a'
import Laundry from '../../assets/images/Laundry.m4a'

const PrevScheduleScreen = () => {
  const navigation = useNavigation();
  const {control, watch} = useForm();
  const keyword = watch('keyword');

  var basketballAudio = new Audio(Basketball)
  var pianoAudio = new Audio(Piano)
  var laundryAudio = new Audio(Laundry)

  const handleBack = () => {
    navigation.navigate('PreviousScheduleScreen')
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
      <Text style={styles.title}>Schedule</Text>
      <br/><br/><br/>
      
      <div style={{display: "inline", width: "100%"}}>
        <Text style={styles.text}>Action Item 1</Text>
        <input value={"Basketball"} style={{height: 35, marginLeft: 18, fontSize: 18, width: 175, marginTop: 24}}></input>
        <img width="100" height="90" style={{}} src={Audio1} onClick={() => basketballAudio.play()}/>
        <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          You have basketball practice today after school
        </ul>
      </div>

      <div style={{display: "inline", width: "100%"}}>
        <Text style={styles.text}>Action Item 2</Text>
        <input value={"Piano"} style={{height: 35, marginLeft: 18, fontSize: 18, width: 175, marginTop: 24}}></input>
        <img width="100" height="90" style={{}} src={Audio1} onClick={() => pianoAudio.play()}/>
        <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          You have piano practice at 5:30 PM today
        </ul>
      </div>

      <div style={{display: "inline", width: "100%"}}>
        <Text style={styles.text}>Action Item 3</Text>
        <input value={"Laundry"} style={{height: 35, marginLeft: 18, fontSize: 18, width: 175, marginTop: 24}}></input>
        <img width="100" height="90" style={{}} src={Audio1} onClick={() => laundryAudio.play()}/>
        <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          Remember to do the laundry before going to sleep
        </ul>
      </div>

      <br/><br/>

      <br/>
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

export default PrevScheduleScreen;