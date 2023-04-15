import React, {useState, useEffect} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import Play from '../../assets/images/playaudio.png';
import Play2 from '../../assets/images/playaudio2.png';
import CustomButton from '../../components/CustomButton/CustomButton'
import { useNavigation } from '@react-navigation/native'
import audio from '../../assets/images/recording.mp3'
import TodayScheduleScreen from '../TodayScheduleScreen/TodayScheduleScreen';
import { useRoute } from '@react-navigation/native';
import { Audio } from "expo-av"

const QuestionScreen = () => {
  const navigation = useNavigation()
  const route = useRoute();
  const keyword = route.params?.keyword;
  const soundUrl = route.params?.soundUrl;
  const answer = route.params?.answer;
  const sound = new Audio.Sound()
  const [playing, setPlaying] = useState();

  const handleBack = () => {
    navigation.navigate('StudentScreen')
  }

  async function playSound() {
    setPlaying(soundUrl)
    console.log(
      "playing audio at " + soundUrl
    )
    await sound.loadAsync({
      uri: soundUrl
    })
    
    await sound.playAsync()
  }

  async function stopSound() {
    await sound.unloadAsync()
    setPlaying(undefined)
  }

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Child Profile</Text>
      <br/><br/>
      <Text style={styles.text3}>Keyword: {keyword}</Text>
      <br/><br/>
      <Text style={styles.text2}>Listen to parent recording:</Text>
      <br/>
      <img width="280" height="280" src={playing ? Play2 : Play} onClick={playing ? stopSound : playSound}/>
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