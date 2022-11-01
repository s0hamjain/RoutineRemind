import { View, Text, TextInput, StyleSheet } from 'react-native';
import React from 'react';
import { Controller } from 'react-hook-form';

const CustomInput = ({control, name, rules = {}, placeholder, secureTextEntry}) => {
  return (
      <Controller 
          control={control}
          name={name}
          rules={rules}
          render={({field: {value, onChange, onBlur}, fieldState: {error}}) => (
            <>
              <View style={[styles.container, {borderColor: error ? 'red' : 'black'},]}>
                <TextInput
                  value={value}
                  onChangeText={onChange}
                  onBlur={onBlur}
                  placeholder={placeholder}
                  style={styles.input}
                  secureTextEntry={secureTextEntry}
                />
              </View>
              {error && (
                <Text style={{color: '#f75745', alignSelf: 'stretch', marginLeft: '2.5%'}}>{error.message || 'Error'}</Text>
              )}
            </>
          )}
      />
  )
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'white',
    width: '95%',
    minHeight: 10,
    borderWidth: 2,
    borderRadius: 5,
    paddingVertical: 15,
    paddingHorizontal: 10,
    marginVertical: 5,
  },
  input: {
    fontSize: 18,
  },
});

export default CustomInput;