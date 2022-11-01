import React, {useState} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import SocialSignInButtons from '../../components/SocialSignInButtons/SocialSignInButtons';
import { useNavigation } from '@react-navigation/native';
import { useForm } from 'react-hook-form';
import { auth } from '../../firebase';

const ForgotPasswordScreen = () => {
  const {control, handleSubmit, watch} = useForm();
  const navigation = useNavigation();
  const em = watch('email');

  const handleForgotPassword = () => {
    auth
      .sendPasswordResetEmail(em)
      .then(
        navigation.navigate('SignIn')
      )
      .then(
        alert("Please check your email to reset your password. If you do not see an email, check your spam folder. Otherwise, type in your email again in the field below.")
      )
  }

  const onSendPressed = data => {
    console.warn(data);
    navigation.navigate('NewPassword');
  }

  const onSignInPressed = () => {
    navigation.navigate('SignIn');
  }

  return (
      <View style = {styles.root}>
        <br/><br/><br/>
        <Text style={styles.title}>Reset Password</Text>     
        <br/><br/><br/>

        <CustomInput
          name="email"
          control={control}
          placeholder="Email"
          rules={{required: 'Email is required'}}
        />

        <CustomButton text="Send" onPress={handleForgotPassword} />
        <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
        <CustomButton text="Back to Sign in" onPress={onSignInPressed} type="TERTIARY" />
        

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
    text: {},
});

export default ForgotPasswordScreen;