%% =========ARDUINO SETUP============= %%

if (serial_port.BytesAvailable > 0)
    [freq, gain] = arduinoRead(serial_port);
end

