import React, {useState, useEffect} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import Logo from '../../assets/images/PersonIcon.png';
import CustomButton from '../../components/CustomButton/CustomButton'
import { useNavigation } from '@react-navigation/native'
import { useRoute } from '@react-navigation/native';

const ParentScreen = () => {
  const [currentDate, setCurrentDate] = useState('')
  const navigation = useNavigation()
  const route = useRoute();
  const keyword = route.params?.keyword;
  const soundUrl = route.params?.soundUrl;
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

  const handlePrevious = () => {
    navigation.navigate('PreviousScheduleScreen')
  }

  const handleToday = () => {
    navigation.navigate('TodayScheduleScreen')
  }

  const handleBack = () => {
    console.log(soundUrl)
    console.log(answer)
    navigation.navigate('ChooseUser', { keyword: keyword, soundUrl: soundUrl, answer: answer });
  }

  return (
    <View style={styles.root}>
      <br/>
      <Text style={styles.title}>Parent Profile</Text>
      <br/>
      <img width="160" height="142" src={Logo} /> 
      <Text style={styles.text}>Today's Date: {currentDate}</Text>
      <br/><br/>
      <CustomButton style="marginBottom: 5px;" text={"View Previous Schedules"} onPress={handlePrevious} type="SCHEDULES"/>
      <CustomButton text={"Edit Today's Schedule"} onPress={handleToday} type="SCHEDULES"/>
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
    height: '100%',
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

export default ParentScreen;