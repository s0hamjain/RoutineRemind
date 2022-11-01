import {useForm, Controller} from 'react-hook-form';
import { View, Text } from 'react-native'
import React, {useEffect} from 'react'
import { StyleSheet } from 'react-native'
import { auth } from '../../firebase'
import { useNavigation } from '@react-navigation/native'
import CustomButton from '../../components/CustomButton/CustomButton'
import CustomInput from '../../components/CustomInput/CustomInput';

const HomeScreen = () => {
  const navigation = useNavigation()
  const {control, formState: {errors}, watch} = useForm();
  const username = watch('user');
  const parent = watch('parent');

  const handleSignOut = () => {
    auth
      .signOut()
      .then(() => {
        location.reload()
      })
      .catch(error => alert(error.message))
  }

  const handleInformation = () => {
    if (username && parent) {
      navigation.navigate('ChooseUser')
    } else {
      alert('Information must be filled in order to proceed')
    }
  }

  return (
    <View style={styles.root}>
      <br/><br/><br/>
      <Text style={styles.title}>Account Information</Text>
      <br/><br/><br/><br/>

      <Text style={styles.text}>Please enter the following information:</Text>
      <br/>
      <CustomInput 
          name="user"
          placeholder="Primary User Full Name"
          control={control}
          rules={{required: 'Primary User Name is required'}}
        />
      
      <CustomInput 
          name="parent"
          placeholder="Parent or Guardian Full Name"
          control={control}
          rules={{required: 'Parent or Guardian Name is required'}}
        />

      <CustomButton text="Submit" onPress={handleInformation} type="PRIMARY"/>
      <br/><br/><br/><br/>
      <CustomButton text="Sign Out" onPress={handleSignOut} type="SECONDARY"/>
    </View>
  )
}

const styles = StyleSheet.create({
  root: {
    backgroundColor: '#364d59',
    height: "100%",
    padding: 20,
    alignItems: 'center',
  },
  title: {
    color: 'white',
    fontSize: 32,
    alignSelf: 'center',
    fontWeight: 'bold',
  },
  text: {
    color: 'white',
    fontSize: '18px',
  },
  line: {
    color: 'white',
    fontFamily: 'monospace',
    fontSize: 40,
    alignSelf: 'center',
    paddingBottom: '10px',
  }
});

export default HomeScreen;