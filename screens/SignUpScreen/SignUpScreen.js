import React, {useState} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import { useNavigation } from '@react-navigation/native';
import { useForm } from 'react-hook-form';
import { auth } from '../../firebase';

const SignUpScreen = () => {

  const {control, handleSubmit, watch} = useForm();
  const pwd = watch('password');
  const confPwd = watch('repeatPassword')
  const navigation = useNavigation();
  const em = watch('email');

  const handleSignUp = () => {
    if (pwd !== confPwd) {
      alert("Passwords do not match");
    } else {
      auth
      .createUserWithEmailAndPassword(em, pwd)
      .then(userCredentials => {
        const user = userCredentials.user;
        console.log(user.em);
      })
      .catch(error => {
        if (error.message === "Firebase: This operation is restricted to administrators only. (auth/admin-restricted-operation).") {
          alert("All information must be filled in.");
        } else if (error.message === "Firebase: An internal AuthError has occurred. (auth/internal-error).") {
          alert("Password must be filled out.");
        } else if (error.message === "Firebase: Password should be at least 6 characters (auth/weak-password).") {
          alert("Password must be at least 6 characters long.");
        } else if (error.message === "Firebase: The email address is already in use by another account. (auth/email-already-in-use).") {
          alert("The email address you enterred is already in use")
        } else if (error.message === "Firebase: The email address is badly formatted. (auth/invalid-email).") {
          alert("Email address is not formatted correctly.");
        } else {
          alert("Unknown Error")
        }
      })
    }
  }

  const onSignInPressed = () => {
    navigation.navigate('SignIn');
  }

  return (
      <View style = {styles.root}>
        <br/><br/><br/>
        <Text style={styles.title}>Create Account</Text>     
        <br/><br/><br/>

        <CustomInput
          name="email"
          placeholder="Email"
          control={control}
          rules={{required: 'Email is required'}}
        />

        <CustomInput
          name="password"
          placeholder="Password"
          control={control}
          secureTextEntry
          rules={{required: 'Password is required', minLength: {value: 8, message: 'Password should be a minimum of 8 characters long'}, maxLength: {value: 24, message: 'Password should be a maximum of 24 characters long'}}}
        />

        <CustomInput
          name="repeatPassword"
          placeholder="Repeat Password"
          control={control}
          secureTextEntry
          rules={{
            required: 'Password must be repeated',
            validate: value => value == pwd || 'Password does not match',
          }}
        />

        <CustomButton text="Register" onPress={handleSignUp} />
        <br/><br/><br/><br/>

        <CustomButton text="Have an account? Sign in" onPress={onSignInPressed} type="TERTIARY" />

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
    logo: {
        width: '70%',
        maxWidth: 300,
        height: 100,
    },
    title: {
      fontSize: 36,
      fontWeight: 'bold',
      color: 'white',
    },
});

export default SignUpScreen;