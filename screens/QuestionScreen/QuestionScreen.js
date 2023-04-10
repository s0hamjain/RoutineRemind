import React, {useState, useEffect} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import Play from '../../assets/images/playaudio.png';
import CustomButton from '../../components/CustomButton/CustomButton'
import { useNavigation } from '@react-navigation/native'
import audio from '../../assets/images/recording.mp3'
import TodayScheduleScreen from '../TodayScheduleScreen/TodayScheduleScreen';
import { useRoute } from '@react-navigation/native';

const QuestionScreen = () => {
  const navigation = useNavigation()
  const route = useRoute();
  const keyword = route.params?.keyword;
  const answer = route.params?.answer;

  const handleBack = () => {
    navigation.navigate('StudentScreen')
  }

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Child Profile</Text>
      <br/><br/>
      <Text style={styles.text2}>Answer: {answer}</Text>
      <br/>
      <Text style={styles.text3}>Keyword: {keyword}</Text>
      <br/><br/>
      <Text style={styles.text2}>Listen to parent recording:</Text>
      <br/>
      <img width="300" height="280" src={Play}/>
      <br/><br/>
      <CustomButton text={"Go Back"} onPress={handleBack} type="SECONDARY"/>
    </View>
  )
}

const styles = StyleSheet.create({
  root: {
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#364d59',
    height: '120%',
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
  text3: {
    marginTop: "10px",
    fontSize: 18,
    color: 'white',
    fontWeight: 'bold',
  },
});

export default QuestionScreen;