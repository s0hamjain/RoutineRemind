import React, {useState} from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import CustomInput from '../../components/CustomInput/CustomInput';
import CustomButton from '../../components/CustomButton/CustomButton';
import SocialSignInButtons from '../../components/SocialSignInButtons/SocialSignInButtons';
import { useNavigation } from '@react-navigation/native';
import { useForm } from 'react-hook-form';

const ConfirmEmailScreen = () => {
  const {control, handleSubmit} = useForm();

  const navigation = useNavigation();

  const onConfirmPressed = data => {
    console.warn(data);
    navigation.navigate('Home')
  }

  const onSignInPressed = () => {
    navigation.navigate('SignIn')
  }

  return (
      <View style = {styles.root}>
        <br/><br/><br/>
        <Text style={styles.title}>Confirm your email</Text>     
        <br/><br/><br/>

        <CustomInput
          name="code"
          control={control}
          placeholder="Enter your confirmation code"
          rules={{
            required: 'Confirmation code is required'
          }}
        />

        <CustomButton
          text="Confirm"
          onPress={handleSubmit(onConfirmPressed)}
        />

        <br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
        <CustomButton
          text="Back to Sign in"
          onPress={onSignInPressed}
          type="TERTIARY"
        />

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

export default ConfirmEmailScreen;