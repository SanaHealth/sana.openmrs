---
title: Administration
---

To administer Sana, you must know how to [install](/Installation "wikilink") and [upgrade](/Upgrading "wikilink") the Sana software. In addition, you should learn a number of more general skills.

Basic Skills
============

In order to implement and maintain Sana, a prospective administrator should become familiar with several component technologies. Knowledge of these topics will be valuable for installation and troubleshooting.

Computer Networking
-------------------

Although this is not meant to be a complete guide to computer networking, we will introduce/review a few points of this subject that are particularly important to Sana. First is the idea of Internet Protocol (IP) addresses. Every computer on a network is assigned an IP address. Computers use these addresses to communicate with each other. An IP address has the form of four numbers from 0 to 254 (inclusive) separated by dots. Website addresses such as sanamobile.org are translated into IP addresses by the Domain Name System, not discussed here.

Many IP addresses and groups of IP addresses are reserved and have particular meanings. For example, 127.0.0.1 is always the local computer. Addresses that start with 192.168 are other computers on a small local network, often several machines sharing a single Internet connection. Any valid IP address that starts with 18. points to a computer at MIT.

Your computer has different IP addresses used for different purposes. As explained above, your computer can always use 127.0.0.1 to refer to itself. Other machines that share an Internet connection with you might address your computer as 192.168.1.10, for example. If your computer is connected directly to the Internet (not through a router), then other computers on a Internet can communicate with your computer using a different IP address assigned by your Internet Service Provider. Websites you visit send data you request to this address. This is called your Public IP address.

If you are behind a router, then the rest of the Internet cannot access your computer directly because your Public IP Address is really assigned to the router, not your computer. Your router must be configured to forward data to the correct Local Area Network (LAN) destination if a Sana server is behind a router. If you use default settings for the Sana server software, you will need to forward network ports 80 and 8080 to the Sana server.

Another important idea is that of Static and Dynamic IP addresses. As the names suggest, static addresses do not change, while dynamic addresses are allocated on the fly. A Sana server needs a Static IP address so that clients know where to send data.

In short, a production Sana server, like any production server intended for public access, needs to have a static public IP address, and any router separating it from the Internet must be configured to forward Sana-related data to it. Achieving this will require a static IP address from your ISP and working knowledge of your local network equipment.

As you become more comfortable with computer networking, you should consult more advanced technical documentation to expand your knowledge.

GNU/Linux
---------

The Sana server software runs on a platform known properly as GNU/Linux, which is Open Source like Sana and has many variants freely available over the Internet. We have standardized our installation around the popular variant Ubuntu Linux, making use of its system layout and package management software. Ubuntu is easy to install even for new users, and using our packages is much easier than installing the software manually. However, note that it is possible to run Sana on essentially any GNU/Linux operating system with enough time and effort.

New GNU/Linux users are often intimidated by the system's Command Line Interface. While powerful graphical environments are well established for these systems and come standard on many variants, including many flavors of Ubuntu, the text-based interface is often the fastest way to get things done. Most GNU/Linux servers run without any graphical environment to conserve resources and for security reasons. It is also simpler to manage servers remotely using the command line. Note that a command line interface is always available, even when a graphical interface is also running.

You should familiarize yourself with basic commands and system administration. In particular, you should begin by learning to navigate the filesystem, manage server daemons, and edit text files using the command line. A simple text editor such as GNU Nano will do the job nicely.

For example, the following command restarts the Tomcat server daemon, which is what runs OpenMRS:

    sudo /etc/init.d/tomcat6 restart

You may also pass "start," "stop," and other arguments as appropriate. Other server daemon control scripts are in this same directory. View all of them using the command to list files:

    ls /etc/init.d

You will notice another script listed there that controls the apache webserver. This is important for managing the Mobile Dispatch Server.

The best way to learn GNU/Linux is to install it, begin using it, and learn how to do things as the need arises. If possible, install it on a personal machine with a full graphical environment to become comfortable with it more quickly.

OpenMRS
-------

OpenMRS runs through Tomcat, which is one of the server daemons you should learn to manage. The [Installation](/Installation "wikilink") process introduces you to the OpenMRS web interface. Beyond following the installation guide, you should explore the administration page and read official documentation to learn to create users and handle other tasks.

Android
-------

The Android platform is designed to be easy to learn and use. An hour or two of exploring the interface will teach you much of what you need to know.

Devices running Android are capable of using many communication networks, including 2G, GPRS, SMS, 3G, and WiFi. Sana relies on GPRS and SMS.

Android applications are packaged in files with the extension .apk. These files can be downloaded from the Android market for free or pay, and if the correct setting is enabled, .apk files can be installed from the Internet or from an SD card. Currently, the Sana client is not listed in the Android market and should be installed manually as described in [Installation](/Installation "wikilink").

Advanced Skills
===============

After a Sana installation is in place, various situations could call for additional skills from the administrator. Bash shell scripting is useful for many general administration tasks. Many software and hardware problems can require MySQL administration, too. Learning more about the Apache and Tomcat web servers will allow you to do more with your server. You are encouraged to learn about these and other subjects as you gain experience as an administrator and, if you wish, learn to become a [Sana Developer](/Development "wikilink").