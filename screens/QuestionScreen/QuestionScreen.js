import React, {useState, useEffect} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import Play from '../../assets/images/playaudio.png';
import CustomButton from '../../components/CustomButton/CustomButton'
import { useNavigation } from '@react-navigation/native'
import audio from '../../assets/images/recording.mp3'

const QuestionScreen = () => {
  const navigation = useNavigation()

  const handleBack = () => {
    navigation.navigate('StudentScreen')
  }

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Child Profile</Text>
      <br/><br/>
      <Text style={styles.text2}>Q: "What time do I have swimming today"</Text>
      <br/>
      <Text style={styles.text}>A: You have swimming today at 3:00 PM</Text>
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
});

export default QuestionScreen;