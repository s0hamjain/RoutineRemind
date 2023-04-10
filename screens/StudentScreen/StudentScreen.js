import React, {useState, useEffect, useRef} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import Logo from '../../assets/images/PersonIcon.png';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import MicRecorder from "mic-recorder-to-mp3"
import { Oval } from "react-loader-spinner"
import axios from "axios"
import Audio1 from '../../assets/images/audio1.png';
import useSpeechToText, { ResultType } from './Hooks';
import { useRoute } from '@react-navigation/native';

const StudentScreen = () => {
  const navigation = useNavigation()
  const [currentDate, setCurrentDate] = useState('')
  const [text, setText] = useState("")
  const route = useRoute();
  const keyword = route.params?.keyword;
  const answer = route.params?.answer;

  useEffect(() => {
    var date = new Date().getDate()
    var month = new Date().getMonth() + 1
    var year = new Date().getFullYear()
    setCurrentDate(
      month + '/' + date + '/' + year
    )
    return () => {}
  }, [])

  const handleBack = () => {
    navigation.navigate('ChooseUser')
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

  const handleSubmit = (error) => {
    console.log(answer)
    navigation.navigate('QuestionScreen', { keyword: keyword, answer: answer });
  }

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Child Profile</Text>
      <br/>
      <img width="145" height="135" src={Logo} /> 
      <Text style={styles.text}>Today's Date: {currentDate}</Text>
      <img width="315" height="280" src={Audio1} onClick={isRecording ? stopSpeechToText : startSpeechToText} />
      <ul style={{color: "white", listStyle: "none", fontFamily: "sans-serif", fontWeight: "bold", fontSize: "18px"}}>
          {(results).map((result) => (
            <li key={result.timestamp}>{result.transcript}</li>
          ))}
          {interimResult && <li>{interimResult}</li>}
      </ul>
      <CustomButton text={"Submit"} onPress={handleSubmit} type="EXTRATWO"/>
      <br/>
      <CustomButton text={"Go Back"} onPress={handleBack} type="SECONDARY"/>
    </View>
  );
}
  

const styles = StyleSheet.create({
  root: {
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#364d59',
    height: '140%',
  },
  title: {
    fontSize: 36,
    fontWeight: 'bold',
    color: 'white',
  },
  text: {
    marginTop: "10px",
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

export default StudentScreen;