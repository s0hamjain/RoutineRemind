import { View, Text } from 'react-native'
import React from 'react'
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

import SignInScreen from '../screens/SignInScreen/SignInScreen';
import SignUpScreen from '../screens/SignUpScreen/SignUpScreen';
import ConfirmEmailScreen from '../screens/ConfirmEmailScreen/ConfirmEmailScreen';
import ForgotPasswordScreen from '../screens/ForgotPasswordScreen/ForgotPasswordScreen';
import NewPasswordScreen from '../screens/NewPasswordScreen/NewPasswordScreen';
import HomeScreen from '../screens/HomeScreen/HomeScreen';
import ChooseUserScreen from '../screens/ChooseUserScreen';
import StudentScreen from '../screens/StudentScreen/StudentScreen';
import ParentScreen from '../screens/ParentScreen/ParentScreen';
import PreviousScheduleScreen from '../screens/PreviousScheduleScreen/PreviousScheduleScreen';
import TodayScheduleScreen from '../screens/TodayScheduleScreen/TodayScheduleScreen';
import QuestionScreen from '../screens/QuestionScreen/QuestionScreen';
import PrevScheduleScreen from '../screens/PrevScheduleScreen/PrevScheduleScreen';
import TodayScheduleScreenTwo from '../screens/TodayScheduleScreenTwo/TodayScheduleScreenTwo';
import TodayScheduleScreenThree from '../screens/TodayScheduleScreenThree/TodayScheduleScreenThree';

const Stack = createNativeStackNavigator();

const Navigation = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{headerShown: false}}>
          <Stack.Screen name="SignIn" component={SignInScreen} />
          <Stack.Screen name="SignUp" component={SignUpScreen} />
          <Stack.Screen name="ConfirmEmail" component={ConfirmEmailScreen} />
          <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
          <Stack.Screen name="NewPassword" component={NewPasswordScreen} />
          <Stack.Screen name="Home" component={HomeScreen} />
          <Stack.Screen name="ChooseUser" component={ChooseUserScreen} />
          <Stack.Screen name="StudentScreen" component={StudentScreen} />
          <Stack.Screen name="ParentScreen" component={ParentScreen} />
          <Stack.Screen name="PreviousScheduleScreen" component={PreviousScheduleScreen} />
          <Stack.Screen name="TodayScheduleScreen" component={TodayScheduleScreen} />
          <Stack.Screen name="TodayScheduleScreenTwo" component={TodayScheduleScreenTwo} />
          <Stack.Screen name="TodayScheduleScreenThree" component={TodayScheduleScreenThree} />
          <Stack.Screen name="QuestionScreen" component={QuestionScreen} />
          <Stack.Screen name="PrevScheduleScreen" component={PrevScheduleScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  )
}

export default Navigation;