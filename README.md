### Sound Stream - Project Description:
The goal of this project was to build an interactive physical interface that could control the articulatory and acoustic parameters of an articulatory speech synthesis in real-time to produce speech sound (vowel sound). The interface was built as a part of course requirement [UBC CPEN 541-Human Interface Technologies](https://courses.ece.ubc.ca/518/), April 2018. Later the project was extended and implemented on [Artisynth](https://www.artisynth.org/Main/HomePage) and presented (poster presentation) at the [176<sup>th</sup> joint meeting of ASA-CAA](https://acousticalsociety.org/176th-meeting-acoustical-society-of-america/), Victoria, BC, Canada.

### Technical Details:
* [JASS SDK](http://persianney.com/kvdoelcsubc/jass/) is used to control the in-built one-dimensional area function based articulatory speech synthesizer.
* The synthesizer is driven by vocal tract parameters (change in area function) and vocal fold parameters (change in source frequency and gain)
* To control the vocal tract parameters, we designed a physical tube whose area could be changed by the user. The interaction is captured by using a document camera and then the change in area is computed in real-time using an image processing algotrithm.
* To control the vocal fold parameters, two interfaces are built and a comparision is made in terms of its flexibility and usability.
  * Mouse based controller (Using a two-dimensional interactive pad)
  * Slider sensor (Based upon slider positions)
  
**Tools Used:** JASS SDK (Java based), Artisynth, MATLAB (image processing toolkit), Processing (p5.js), Computer mouse, Arduino, Phidgets Slider Sensor, Document camera and others (boards, paper, black tape etc.) 

### How To Use:
* Download the [JASS SDK](http://persianney.com/kvdoelcsubc/jass/) and configure it using [Eclipse](https://www.eclipse.org/downloads/packages/release/oxygen/3a/eclipse-ide-java-developers) or anyother IDE.
* Update the `VTNTDemo.java` file with the provided code.
* Experiment with different source-controller options (Slider sensor/Mouse) by commenting out either of them.

```
                    	// SLIDER CONTROLLER
//                    	valRosenberg[0] = ((double[]) proxy.getVariable("freq"))[0];
//                    	valRosenberg[3] = ((double[]) proxy.getVariable("gain"))[0];
                    	// END
                    	
                    	// MOUSE CONTROLLER
                    	valRosenberg[0] = (sketch_obj.circleX/675)*1000;
                    	valRosenberg[3] = sketch_obj.circleY/675;
                     	//END
 ```
**[Note]** I have not provided the code for reading slider sensor output using Arduino as you might be using different I/O PINs and the code is very simple and straight-forward.

### Project Contributors:
 Debasish Ray Mohapatra, [Pramit Saha](https://github.com/Pramit15), [Praneeth SV](https://github.com/Praneethsv)

*My Contribution: Helped in designing the physical tube (vocal tract), arduino programming (to feed data to JASS SDK/Artisynth), Design the mouse-controlled source-model (vocal fold) using [Processing](https://processing.org/) in java environemnt*

### References:

*For more details please follow these papers:*

[1] SOUND STREAM: Towards vocal sound synthesis via dual-handed simultaneouscControl of articulatory parameters. [Course Submission](https://github.com/Debasishray19/SoundStream/blob/master/assets/soundstream_classroomSubmission.pdf), [Classroom Presentation](https://github.com/Debasishray19/SoundStream/blob/master/assets/ConferencePresentation.pdf)

[2] SOUND-STREAM II: Towards real-time gesture-controlled articulatory sound synthesis. [Conference Paper](https://arxiv.org/pdf/1811.08029.pdf), 176th joint meeting of ASA-CAA conference, Victoria, BC Canada.

### Demo:
* The video (Not professional level) demonstrate the working principle of the interface. [Demo](https://github.com/Debasishray19/SoundStream/blob/master/assets/videoDemo.mp4)

* A short demo presentation. [Presentation](https://github.com/Debasishray19/SoundStream/blob/master/assets/DemoSession.pdf)

### Contact Info: 
*In case of any queries please contact - debasishiter@gmail.com*
