function [double_slider1, double_slider2] = arduinoRead(serial_port)
    
    % Read serial_port value
    slider1 = fgets(serial_port);
    slider2 = fgets(serial_port);
    
    %Convertinf string value to interger
    double_slider1 = str2double(slider1);
    double_slider2 = str2double(slider2);
    
    if((double_slider1>200)&&(double_slider1<300))
        double_slider1=200;
    end
    
   if((double_slider1>400)&&(double_slider1<600))
        double_slider1=400;
   end
    
   if(double_slider1>1000)
       double_slider1=1000;
   end
    
    double_slider2 = (double_slider2/1023);
    
end