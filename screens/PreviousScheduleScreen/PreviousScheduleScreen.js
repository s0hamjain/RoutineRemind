import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native'
import CustomButton from '../../components/CustomButton/CustomButton';

const PreviousScheduleScreen = () => {
  const navigation = useNavigation()

  const today = new Date();
  const formattedDate = `${today.getMonth() + 1}/${today.getDate()}/${today.getFullYear()}`;

  const handleBack = () => {
    navigation.navigate('ParentScreen')
  }

  const handlePrev = () => {
    navigation.navigate('PrevScheduleScreen')
  }

  const handleMore = () => {
    navigation.navigate('MorePreviousScheduleScreen')
  }

  return (
    <ScrollView>
      <View style={styles.root}>
        <br/>
        <Text style={styles.title}>Past Schedules</Text>
        <br/><br/><br/>
        <CustomButton text={`${today.getMonth() + 1}/${today.getDate() - 1}/${today.getFullYear()}`} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={`${today.getMonth() + 1}/${today.getDate() - 2}/${today.getFullYear()}`} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={`${today.getMonth() + 1}/${today.getDate() - 3}/${today.getFullYear()}`} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={`${today.getMonth() + 1}/${today.getDate() - 4}/${today.getFullYear()}`} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={`${today.getMonth() + 1}/${today.getDate() - 5}/${today.getFullYear()}`} onPress={handlePrev} type="EXTRA"/>
        <CustomButton text={"See more   ▼"} onPress={handleMore} type="TERTIARY"/>
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