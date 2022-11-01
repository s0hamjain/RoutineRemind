import { View, Text } from 'react-native'
import React from 'react'
import CustomButton from '../CustomButton/CustomButton'

const SocialSignInButtons = () => {


  const onSignInGoogle = () => {
    alert("google");
  };

  const onSignInApple = () => {
    console.warn("Sign in Apple");
  };

  const onSignInFacebook = () => {
    console.warn("Sign in Facebook");
  };

  return (
    <>
      <CustomButton
          text="Sign in with Google"
          onPress={onSignInGoogle}
          bgColor="#FAE9EA"
          fgColor="#DD4D44"
        />
    </>
  )
}

export default SocialSignInButtons