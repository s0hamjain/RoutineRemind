import React, {useState, useEffect} from 'react';
import { View, Text, Image, StyleSheet, useWindowDimensions, ScrollView, TextInput } from 'react-native';
import Logo from '../../assets/images/routineremindlogo.png';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import {useForm, Controller} from 'react-hook-form';
import { auth } from '../../firebase';

const SignInScreen = () => {

  const navigation = useNavigation();
  const {control, handleSubmit, formState: {errors}, watch} = useForm();
  const em = watch('email');
  const pwd = watch('password');

  useEffect(() => {
    auth.onAuthStateChanged(user => {
      if (user) {
        navigation.navigate('ChooseUser')
      }
    })
  }, [])

  const handleLogin = () => {
    auth
      .signInWithEmailAndPassword(em, pwd)
      .then(userCredentials => {
        const user = userCredentials.user;
        console.log(user.em);
      })
      .catch(error => {
        if (error.message === "Firebase: Error (auth/missing-email).") {
          alert("Email address is required")
        } else if (error.message === "Firebase: An internal AuthError has occurred. (auth/internal-error).") {
          alert("Password is required")
        } else if (error.message === "Firebase: The email address is badly formatted. (auth/invalid-email).") {
          alert("Email address is not formatted correctly")
        } else if (error.message === "Firebase: There is no user record corresponding to this identifier. The user may have been deleted. (auth/user-not-found).") {
          alert("User login is invalid")
        } else if (error.message === "FirebaseError: Firebase: The password is invalid or the user does not have a password. (auth/wrong-password).") {
          alert("User login is invalid")
        } else {
          alert("Invalid login")
        }
      })
  }

  const onSignInPressed = (data) => {
    console.log(data); 
    navigation.navigate('Home');
  }
  
  const onForgotPasswordPressed = () => {
    navigation.navigate('ForgotPassword');
  }

  const onSignUpPressed = () => {
    navigation.navigate('SignUp');
  }

  const{height} = useWindowDimensions();

  return (
      <View style = {styles.root}>
      <br/><br/>
        <img width="395" height="122" src={Logo} />    
        <br/><br/>
        
        <CustomInput 
          name="email"
          placeholder="Email"
          control={control}
          rules={{required: 'Email is required'}}
        />
        <CustomInput 
          name="password"
          placeholder="Password"
          secureTextEntry
          control={control}
          rules={{required: 'Password is required', minLength: {value: 8, message: 'Password is a minimum of 8 characters long'}}}
        />

        <CustomButton text="Sign In" onPress={handleLogin} />

        <br/><br/><br/><br/>

        <CustomButton text="Forgot password?" onPress={onForgotPasswordPressed} type="TERTIARY" />
        <CustomButton text="Don't have an account? Create one" onPress={onSignUpPressed} type="TERTIARY" />

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
});

export default SignInScreen;