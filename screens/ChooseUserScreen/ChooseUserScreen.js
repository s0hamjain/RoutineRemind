import React, {useState, useEffect} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CustomButton from '../../components/CustomButton/CustomButton'
import CustomInput from '../../components/CustomInput/CustomInput';
import {useForm, Controller} from 'react-hook-form';
import { auth } from '../../firebase'
import { useNavigation } from '@react-navigation/native'
import HomeScreen from '../HomeScreen/HomeScreen';

const ChooseUserScreen = () => {
  const navigation = useNavigation()
  const handleSignOut = () => {
      auth
        .signOut()
        .then(() => {
          location.reload()
        })
        .catch(error => alert(error.message))
  }

  const handleParent = () => {
    navigation.navigate('ParentScreen')
  }

  const handleStudent = () => {
    navigation.navigate('StudentScreen')
  }

  return (
      <View style = {styles.root}>
        <br/><br/><br/>
        <Text style={styles.title}>Choose User</Text>   
        <br/><br/><br/><br/>
        <CustomButton style="marginBottom: 5px;" text={"Parent"} onPress={handleParent} type="EXTRA"/>
        <CustomButton text={"Child"} onPress={handleStudent} type="EXTRA"/>
        <br/><br/><br/><br/>
        <CustomButton text="Sign Out" onPress={handleSignOut} type="SECONDARY"/>
      </View>
  );
};

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
});

export default ChooseUserScreen;