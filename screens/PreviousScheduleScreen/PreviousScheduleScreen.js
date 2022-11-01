import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native'
import CustomButton from '../../components/CustomButton/CustomButton';

const PreviousScheduleScreen = () => {
  const navigation = useNavigation()

  const handleBack = () => {
    navigation.navigate('ParentScreen')
  }

  const handlePrev = () => {
    navigation.navigate('PrevScheduleScreen')
  }

  return (
    <ScrollView>
      <View style={styles.root}>
        <br/>
        <Text style={styles.title}>Past Schedules</Text>
        <br/><br/><br/>
        <CustomButton text={"10/29"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/28"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/27"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/26"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/25"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/24"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/23"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/22"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/21"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"10/20"} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"See more   ▼"} onPress={handlePrev} type="TERTIARY"/>
        <br/><br/>
        <CustomButton text={"Go Back"} onPress={handleBack} type="SECONDARY"/>
        <br/><br/><br/><br/><br/><br/>
      </View>
    </ScrollView>
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

export default PreviousScheduleScreen;