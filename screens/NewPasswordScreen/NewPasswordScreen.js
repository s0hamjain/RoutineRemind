import React, {useState} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import SocialSignInButtons from '../../components/SocialSignInButtons/SocialSignInButtons';
import { useNavigation } from '@react-navigation/native';
import { useForm } from 'react-hook-form';

const NewPasswordScreen = () => {
  const {control, handleSubmit} = useForm();
  
  const navigation = useNavigation();

  const onSubmitPressed = data => {
    console.warn(data);
    navigation.navigate('Home');
  }

  const onSignInPressed = () => {
    navigation.navigate('SignIn')
  }

  return (
      <View style = {styles.root}>
        <br/><br/><br/>
        <Text style={styles.title}>Reset Password</Text>     
        <br/><br/><br/>
        <CustomInput name="code" placeholder="Code" control={control} rules={{required: 'Code is required'}}/>
        <CustomInput name="newPassword" placeholder="Enter your new password" control={control} secureTextEntry rules={{required: 'New password is required', minLength: {value: 8, message: 'Password should be a minimum of 8 characters long'}, maxLength: {value: 24, message: 'Password should be a maximum of 24 characters long'}}} />
        <CustomButton text="Submit" onPress={handleSubmit(onSubmitPressed)} />

        <br/><br/><br/><br/><br/><br/><br/>
        <CustomButton text="Back to Sign in" onPress={onSignInPressed} type="TERTIARY" />

      </View>
  );
};

const styles = StyleSheet.create({
    root: {
      alignItems: 'center',
      padding: 20,
      backgroundColor: '#364d59',
      height: "100%",
    },
    title: {
      fontSize: 36,
      fontWeight: 'bold',
      color: 'white',
    },
    text: {},
});

export default NewPasswordScreen;