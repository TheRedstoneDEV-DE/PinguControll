package me.theredstonedevde.pawebctl;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sound.midi.MetaEventListener;

import me.theredstonedevde.mprisctl.MPRISctl;
import me.theredstonedevde.types.Metadata;

public class WebServer {
    private static final String HOST_ADDRESS = "192.168.0.107";
    private static final String PORT = "8193";
    private static final String[] APPROVED_IP_ADDRESSES = {
        HOST_ADDRESS,
        "192.168.0.112",
        "127.0.0.1"
    };
    public static void main(String[] args) { // Change this to your desired port
        try {
            // Create a server socket to listen on the specified port
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(PORT));
            System.out.println("Server is running on port " + PORT);
            MPRISctl mpris = new MPRISctl();
            mpris.init();
            while (true) {
                // Wait for client connection
                Socket clientSocket = serverSocket.accept();

                // Create a new thread to handle the client request
                Thread thread = new Thread(new RequestHandler(clientSocket, mpris));
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class RequestHandler implements Runnable {
        private Socket clientSocket;
        private MPRISctl mpris;

        public RequestHandler(Socket clientSocket, MPRISctl mpris) {
            this.clientSocket = clientSocket;
            this.mpris=mpris;
        }

        @Override
        public void run() {
            //System.out.println("Request incomming...");
            try {
                // Create input and output streams for the client socket
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);


                // Read the HTTP request from the client
                String request = in.readLine();

                if (request!=null && isIPAddressApproved(clientSocket.getRemoteSocketAddress().toString())) {
                    if (request.startsWith("GET / HTTP")) {
                        //System.out.println("Type: GET\nLocation: /");
                        // Handle GET request on "/"
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response += "<html><head>";
                        response += """
                        <style>
                         body{
                                background-color: black;
                                color: cyan;
                                font-family: monospace
                            }
                            .slidecontainer {
                              width: 100%;
                            }
                            
                            .slider {
                              -webkit-appearance: none;
                              width: 100%;
                              height: 5px;
                              background: #262626;
                              outline: none;
                              opacity: 0.7;
                              -webkit-transition: .2s;
                              transition: opacity .2s;
                            }
                            
                            .slider:hover {
                              opacity: 1;
                            }
                            
                            .slider::-webkit-slider-thumb {
                              -webkit-appearance: none;
                              appearance: none;
                              width: 25px;
                              height: 25px;
                              background: cyan;
                              cursor: pointer;
                            }
                            
                            .slider::-moz-range-thumb {
                              width: 25px;
                              height: 25px;
                              background: #04AA6D;
                              cursor: pointer;
                            }
                            button,.submit{
                                background-color: transparent;
                                color: #009ba3;
                                top: 50%;
                                padding: 10px;
                                border-color: #009ba3;
                                border-style: solid;
                                transition: 0.5s;
                                font-size: 1.2em;
                              }
                            
                              button:hover,.submit:hover{
                                background-color: transparent;
                                color: #03e9f4;
                                top: 50%;
                                padding: 10px;
                                border-color: #03e9f4;
                                border-style: solid;
                                transition: 0.5s;
                                font-size: 1.2em;
                              }
                            
                              button:active,.submit:active{
                                border-color: #c2fcff;
                                color: #c2fcff;
                                transition: 0.0s;
                                font-size: 1.2em;
                              }
                              .submit {
                                width:100%;
                                height: 50px;
                                font-size: 1.5em;
                              }
                              .submit:hover {
                                font-size: 1.5em;
                              }
                              .submit:active {
                                font-size: 1.5em;
                              }
                            </style>
                            <script>
                            function onload(){
                                var sliders = document.getElementsByClassName("slider");
                                var output = document.getElementById("demo");
                                for (var i = 0; i < sliders.length; i++) {
                                    sliders[i].oninput = function() {
                                        output.innerHTML = this.value+"%";
                                    }
                                }
                            }
                            </script>
                            <title>RAM</title>
                            <meta http-equiv = \"refresh\" content = \"10; url = http://%host%:%port%\" />
                                </head><body onload="onload()"><center><h1>Remote Audio Mixer</h1></center><form action="/post" method="post">""".replaceAll("%host%", HOST_ADDRESS).replaceAll("%port%", PORT);
                        PulseAudioSinkInfo pasi = new PulseAudioSinkInfo();
                        PulseAudioSinkInputInfo pasii = new PulseAudioSinkInputInfo();
                        List<SinkInput> sis = Stream.concat(pasi.get().stream(), pasii.get().stream()).toList();
                        for(SinkInput si : sis){
                            if (si.type == 0){
                                response += "<h3>"+si.name+"</h3><input type=\"range\" min=\"0\" max=\"150\" value=\""+Integer.toString(si.volume)+"\" name=\""+si.id+"\" class=\"slider\">";
                            }else {
                                response += "<h3>"+si.name+"</h3><input type=\"range\" min=\"0\" max=\"150\" value=\""+Integer.toString(si.volume)+"\" name=\"#"+si.id+"\" class=\"slider\">";
                            }
                        }
                        response += "<h3 id=\"demo\">--%</h3><div class=\"submit-area\">\n" + //
                                "          <input type=\"submit\" value=\"Submit\" class=\"submit\">\n" + //
                                "        </div>";
                        response += "</form><div class=\"submit-area\"><a href=\"/mpris\"><input type=\"submit\" value=\"MPRIS\" class=\"submit\"></a></div></body></html>";
                        out.print(response);
                        out.flush();
                    } else if (request.startsWith("POST /post HTTP")) {
                        // System.out.println("Type: POST\nLocation: /post");
                        // Handle POST request on "/post"
                        int contentLength = 0;
                        String line;
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            if (line.startsWith("Content-Length: ")) {
                                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                            }
                        }
                        // Read the POST data based on the Content-Length
                        StringBuilder requestBody = new StringBuilder();
                        for (int i = 0; i < contentLength; i++) {
                            requestBody.append((char) in.read());
                        }

                        // Parse POST data (assuming it's in the form of key-value pairs)
                        Map<String, String> postData = parsePostData(requestBody.toString());

                        PulseAudioSinkInfo pasi = new PulseAudioSinkInfo();
                        PulseAudioSinkInputInfo pasii = new PulseAudioSinkInputInfo();
                        List<SinkInput> sis = Stream.concat(pasi.get().stream(), pasii.get().stream()).toList();
                        Map<String, String> sinks = new HashMap<String, String>();
                        for ( SinkInput si : sis ){
                            if (si.type==0){
                                sinks.put(si.id,Integer.toString(si.volume));
                            }else{
                                sinks.put("#"+si.id,Integer.toString(si.volume));
                            }
                        }
                        Controller ctrler= new Controller();
                        for (Map.Entry<String, String> entry : postData.entrySet()) {
                            if(sinks.get(entry.getKey())!=entry.getValue()){
                                if (entry.getKey().startsWith("#")){
                                    ctrler.setVolume(entry.getKey().replace("#", ""), 1, entry.getValue());
                                }else{
                                    ctrler.setVolume(entry.getKey(), 0, entry.getValue());
                                }
                            }
                        }
                        // Build a simple HTML response
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response += "<html><head>";
                        response += "<meta http-equiv = \"refresh\" content = \"0; url = http://%host%:%port%\" />";
                        response += """
                                    <style>
                                    html{
                                        background-color: #000b0c;
                                        color: #00d2e6;
                                        font-family: monospace;
                                    }
                                    </style>
                                    """;
                        response += "</body></head>";
                        out.print(response.replaceAll("%host%",HOST_ADDRESS).replaceAll("%port%",PORT));
                        out.flush();
                    } else if (request.startsWith("GET /mpris HTTP")) {
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response+="""
                            <html>
                            <head>
                                <style>
                                    body{
                                        background-color: #000b0c;
                                        color: #00d2e6;
                                        font-family: monospace;
                                    }
                                    h1{
                                        text-align: center;
                                        font-size: 2em;
                                    }
                                    img{
                                        box-shadow: 4px 4px 8px rgba(0, 0, 0, 0.2);
                                        margin-left: 32.5%;
                                        border-radius:20px;
                                        z-index: 2;
                                    }
                                    .player-controls{
                                        margin-top: 2%;
                                    }
                                    svg{
                                        margin-right:12.5%;
                                    }
                                    .firstsvg{
                                        margin-left: 35%;
                                    }
                                    .credits{
                                        margin-top: 2%;
                                        text-align: center;
                                    }
                                    .credits .title{
                                        font-size: 1.4em;
                                        font-weight: 700;
                                    }
                                    .credits .artist{
                                        font-weight: 300;
                                        font-size: 1.3em;
                                    }
                                    a {
                                        color:#00d2e6
                                    }

                                   

                               
                               @import url(https://fonts.googleapis.com/css?family=Arimo:400,400italic,700,700italic);
                                    body,
                                    html {
                                    height: 100%;
                                    padding: 0;
                                    margin: 0;
                                    font-family: 'Arimo', sans-serif;
                                    }

                                    main {
                                    z-index: 2;
                                    position: relative;
                                    height: 100%;
                                    background-color: #000b0c;
                                    -webkit-transition: transform .7s ease-in-out;
                                    -moz-transition: transform .7s ease-in-out;
                                    -ms-transition: transform .7s ease-in-out;
                                    -o-transition: transform .7s ease-in-out;
                                    transition: transform .7s ease-in-out;
                                    }

                                    .sidebar {
                                    height: 100%;
                                    width: 400px;
                                    position: fixed;
                                    top: 0;
                                    z-index: 1;
                                    right: 0;
                                    background-color: #000b1c;
                                    opacity:0.9;
                                    }

                                    .bar {
                                    display: block;
                                    height: 3px;
                                    width: 30px;
                                    background-color: #fff;
                                    margin: 10px auto;
                                    }

                                    .button {
                                    cursor: pointer;
                                    display: inline-block;
                                    width: auto;
                                    margin: 0 auto;
                                    -webkit-transition: all .7s ease;
                                    -moz-transition: all .7s ease;
                                    -ms-transition: all .7s ease;
                                    -o-transition: all .7s ease;
                                    transition: all .7s ease;
                                    }

                                    .nav-right {
                                    position: fixed;
                                    right: 40px;
                                    top: 20px;
                                    }

                                    .nav-right.visible-xs {
                                    z-index: 3;
                                    }

                                    .hidden-xs {
                                    display: none;
                                    }

                                    .middle {
                                    margin: 0 auto;
                                    }

                                    .bar {
                                    -webkit-transition: all .7s ease;
                                    -moz-transition: all .7s ease;
                                    -ms-transition: all .7s ease;
                                    -o-transition: all .7s ease;
                                    transition: all .7s ease;
                                    }

                                    .nav-right.visible-xs .active .bar {
                                    background-color: #FFF;
                                    -webkit-transition: all .7s ease;
                                    -moz-transition: all .7s ease;
                                    -ms-transition: all .7s ease;
                                    -o-transition: all .7s ease;
                                    transition: all .7s ease;
                                    }

                                    .button.active .top {
                                    -webkit-transform: translateY(15px) rotateZ(45deg);
                                    -moz-transform: translateY(15px) rotateZ(45deg);
                                    -ms-transform: translateY(15px) rotateZ(45deg);
                                    -o-transform: translateY(15px) rotateZ(45deg);
                                    transform: translateY(15px) rotateZ(45deg);
                                    }

                                    .button.active .bottom {
                                    -webkit-transform: translateY(-15px) rotateZ(-45deg);
                                    -moz-transform: translateY(-15px) rotateZ(-45deg);
                                    -ms-transform: translateY(-15px) rotateZ(-45deg);
                                    -o-transform: translateY(-15px) rotateZ(-45deg);
                                    transform: translateY(-15px) rotateZ(-45deg);
                                    }

                                    .button.active .middle {
                                    width: 0;
                                    }

                                    .move-to-left {
                                    -webkit-transform: translateX(-400px);
                                    -moz-transform: translateX(-400px);
                                    -ms-transform: translateX(-400px);
                                    -o-transform: translateX(-400px);
                                    transform: translateX(-400px);
                                    }

                                    nav {
                                    padding-top: 30px;
                                    }

                                    .sidebar-list {
                                    padding: 0;
                                    margin: 0;
                                    list-style: none;
                                    position: relative;
                                    margin-top: 150px;
                                    text-align: center;
                                    }

                                    .sidebar-item {
                                    margin: 30px 0;
                                    opacity: 0;
                                    -webkit-transform: translateY(-20px);
                                    -moz-transform: translateY(-20px);
                                    -ms-transform: translateY(-20px);
                                    -o-transform: translateY(-20px);
                                    transform: translateY(-20px);
                                    }

                                    .sidebar-item:first-child {
                                    -webkit-transition: all .7s .2s ease-in-out;
                                    -moz-transition: all .7s .2s ease-in-out;
                                    -ms-transition: all .7s .2s ease-in-out;
                                    -o-transition: all .7s .2s ease-in-out;
                                    transition: all .7s .2s ease-in-out;
                                    }

                                    .sidebar-item:nth-child(2) {
                                    -webkit-transition: all .7s .4s ease-in-out;
                                    -moz-transition: all .7s .4s ease-in-out;
                                    -ms-transition: all .7s .4s ease-in-out;
                                    -o-transition: all .7s .4s ease-in-out;
                                    transition: all .7s .4s ease-in-out;
                                    }

                                    .sidebar-item:nth-child(3) {
                                    -webkit-transition: all .7s .6s ease-in-out;
                                    -moz-transition: all .7s .6s ease-in-out;
                                    -ms-transition: all .7s .6s ease-in-out;
                                    -o-transition: all .7s .6s ease-in-out;
                                    transition: all .7s .6s ease-in-out;
                                    }

                                    .sidebar-item:last-child {
                                    -webkit-transition: all .7s .8s ease-in-out;
                                    -moz-transition: all .7s .8s ease-in-out;
                                    -ms-transition: all .7s .8s ease-in-out;
                                    -o-transition: all .7s .8s ease-in-out;
                                    transition: all .7s .6s ease-in-out;
                                    }

                                    .sidebar-item.active {
                                    opacity: 1;
                                    -webkit-transform: translateY(0px);
                                    -moz-transform: translateY(0px);
                                    -ms-transform: translateY(0px);
                                    -o-transform: translateY(0px);
                                    transform: translateY(0px);
                                    }

                                    .sidebar-anchor {
                                    color: #FFF;
                                    text-decoration: none;
                                    font-size: 1.8em;
                                    text-transform: uppercase;
                                    position: relative;
                                    padding-bottom: 7px;
                                    }

                                    .sidebar-anchor:before {
                                    content: "";
                                    width: 0;
                                    height: 2px;
                                    position: absolute;
                                    bottom: 0;
                                    left: 0;
                                    background-color: #FFF;
                                    -webkit-transition: all .7s ease-in-out;
                                    -moz-transition: all .7s ease-in-out;
                                    -ms-transition: all .7s ease-in-out;
                                    -o-transition: all .7s ease-in-out;
                                    transition: all .7s ease-in-out;
                                    }

                                    .sidebar-anchor:hover:before {
                                    width: 100%;
                                    }

                                    .ua {
                                    font-size: 2.5em;
                                    position: absolute;
                                    bottom: 20px;
                                    margin-left: 20px;
                                    color: #000b1c;
                                    }

                                    .fa {
                                    font-size: 4.4em;
                                    color: #00d2e6;
                                    -webkit-transition: all 1s ease;
                                    -moz-transition: all 1s ease;
                                    -ms-transition: all 1s ease;
                                    -o-transition: all 1s ease;
                                    transition: all 1s ease;
                                    }

                                    .ua:hover .fa {
                                    color: #FFF;
                                    -webkit-transform: scale(1.3);
                                    -moz-transform: scale(1.3);
                                    -ms-transform: scale(1.3);
                                    -o-transform: scale(1.3);
                                    transform: scale(1.3);
                                    -webkit-transition: all 1s ease;
                                    -moz-transition: all 1s ease;
                                    -ms-transition: all 1s ease;
                                    -o-transition: all 1s ease;
                                    transition: all 1s ease;
                                    }

                                    @media (min-width: 480px) {
                                    .nav-list {
                                        display: block;
                                    }
                                    }

                                    @media (min-width: 768px) {
                                    .nav-right {
                                        position: absolute;
                                    }
                                    .hidden-xs {
                                        display: block;
                                    }
                                    .visible-xs {
                                        display: none;
                                    }
                                    }
                                        .glow-container {
                                            position: relative;
                                        }

                                        .glow-container::before {
                                            content: "";
                                            position: absolute;
                                            top: 0;
                                            left: 0;
                                            right: 0;
                                            bottom: 0;
                                            background-image: url('%artwork%'); /* Hintergrundbild ersetzen */
                                            filter: blur(130px);
                                            opacity: 0.7;
                                          }

                                </style>
                                <script>
                                const HttpReq = new XMLHttpRequest();
                                const url='http://%host%:%port%/mpris?id';
                                var lastsum = "none"
                                //Http.open("GET", url);
                                setInterval(function run() {
                                    HttpReq.open("GET",url);
                                    HttpReq.send();
                                  }, 1000);
                            
                                HttpReq.onreadystatechange = (e) => {
                                 if(lastsum=="none" || lastsum==""){
                                    lastsum=HttpReq.responseText;
                                 }
                                 var newsum=HttpReq.responseText
                                 if(lastsum!=newsum && newsum!=''){
                                    location.reload();
                                    console.log("rel!")
                                 }
                                 //console.log(newsum);
                                 //console.log(lastsum);
                                }
                                </script>
                                   <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css">
                                <meta name="viewport" content="width=device-width, initial-scale=1" />
                            </head>
                            <body>
                         
                             <div class="nav-right visible-xs">
                                    <div class="button" id="btn">
                                        <div class="bar top"></div>
                                        <div class="bar middle"></div>
                                        <div class="bar bottom"></div>
                                    </div>
                                </div>
                                <!-- nav-right -->
                                <main>
                                    <nav>
                                        <div class="nav-right hidden-xs">
                                            <div class="button" id="btn">
                                                <div class="bar top"></div>
                                                <div class="bar middle"></div>
                                                <div class="bar bottom"></div>
                                            </div>
                                        </div>
                                        <!-- nav-right -->
                                    </nav>

                                <h1>MPRIS-Controller</h1>
                               <div class="glow-container">
                                <img src="%artwork%" height="auto" width="35%"></img>
                               </div>
                                <div class="credits">
                                    <a class="title">%title%</a><br>
                                    <a class="artist">%artist%</a>
                                </div>
                                <div class="player-controls">
                                    <a href="?prev" ><svg class="firstsvg" xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-player-skip-back-filled" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                                        <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                                        <path d="M19.496 4.136l-12 7a1 1 0 0 0 0 1.728l12 7a1 1 0 0 0 1.504 -.864v-14a1 1 0 0 0 -1.504 -.864z" stroke-width="0" fill="currentColor" />
                                        <path d="M4 4a1 1 0 0 1 .993 .883l.007 .117v14a1 1 0 0 1 -1.993 .117l-.007 -.117v-14a1 1 0 0 1 1 -1z" stroke-width="0" fill="currentColor" />
                                    </svg></a>
                                    <a href="?play" ><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-player-play-filled" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                                        <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                                        <path d="M6 4v16a1 1 0 0 0 1.524 .852l13 -8a1 1 0 0 0 0 -1.704l-13 -8a1 1 0 0 0 -1.524 .852z" stroke-width="0" fill="currentColor" />
                                    </svg></a>
                                    <a href="?next" ><svg xmlns="http://www.w3.org/2000/svg" class="icon icon-tabler icon-tabler-player-skip-forward-filled" width="24" height="24" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
                                        <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                                        <path d="M3 5v14a1 1 0 0 0 1.504 .864l12 -7a1 1 0 0 0 0 -1.728l-12 -7a1 1 0 0 0 -1.504 .864z" stroke-width="0" fill="currentColor" />
                                        <path d="M20 4a1 1 0 0 1 .993 .883l.007 .117v14a1 1 0 0 1 -1.993 .117l-.007 -.117v-14a1 1 0 0 1 1 -1z" stroke-width="0" fill="currentColor" />
                                    </svg></a>
                                </div>
                                    <a href="http://%host%:%port%" class="ua" target="_blank">
                                        <i class="fa fa-angle-left"></i>
                                    </a>
                                </main>

                                <div class="sidebar">
                                    <ul class="sidebar-list">
                                        <li class="sidebar-item"><a href="#" class="sidebar-anchor">Mpris </a></li>
                                        <li class="sidebar-item"><a href="http://%host%:%port%/mpris" class="sidebar-anchor">Mixer</a></li>
                                    </ul>
                                </div>
                                <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/2.2.2/jquery.min.js"></script>
                                <script>
                                $(document).ready(function() {

                                    function toggleSidebar() {
                                        $(".button").toggleClass("active");
                                        $("main").toggleClass("move-to-left");
                                        $(".sidebar-item").toggleClass("active");
                                    }

                                    $(".button").on("click tap", function() {
                                        toggleSidebar();
                                    });

                                    $(document).keyup(function(e) {
                                        if (e.keyCode === 27) {
                                        toggleSidebar();
                                        }
                                    });

                                    });
                                </script>
                            </body>
                        </html>
                                """.replaceAll("%host%", HOST_ADDRESS).replaceAll("%port%",PORT);
                        Metadata meta = mpris.getMetadata();
                        if (meta.getArtwork()!= null){
                            if (meta.getArtwork().contains("80x80")){
                                response=response.replaceAll("%artwork%", meta.getArtwork().replaceAll("80x80", "200x200"));
                            }else{
                                response=response.replaceAll("%artwork%", meta.getArtwork());
                            }
                        }
                        if (meta.getTitle()!=null){
                            response=response.replaceAll("%title%", meta.getTitle());
                        }else{
                            response=response.replaceAll("%title%", "Unknown");
                        }
                        if (meta.getArtist()!=null){
                            response=response.replaceAll("%artist%", meta.getArtist());
                        }else{
                            response=response.replaceAll("%artist%", "Unknown");
                        }
                        out.print(response);
                        out.flush();
                        
                    } else if (request.startsWith("GET /mpris?prev HTTP")) {
                        mpris.prev();
                        // Build a simple HTML response
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response += "<html><head>";
                        response += "<meta http-equiv = \"refresh\" content = \"1; url = http://%host%:%port%/mpris\" />"; //TODO: change URL
                        response += """
                                    <style>
                                    html{
                                        background-color: #000b0c;
                                        color: #00d2e6;
                                        font-family: monospace;
                                    }
                                    </style>
                                    """;
                        response += "</head>";
                        out.print(response.replaceAll("%host%",HOST_ADDRESS).replaceAll("%port%",PORT));
                        out.flush();                    
                    } else if (request.startsWith("GET /mpris?play HTTP")) {
                        mpris.playpause();
                        // Build a simple HTML response
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response += "<html><head>";
                        response += "<meta http-equiv = \"refresh\" content = \"0; url = http://%host%:%port%/mpris\" />"; //TODO: change URL
                        response += """
                                    <style>
                                    html{
                                        background-color: #000b0c;
                                        color: #00d2e6;
                                        font-family: monospace;
                                    }
                                    </style>
                                    """;
                        response += "</body></head>";
                        out.print(response.replaceAll("%host%",HOST_ADDRESS).replaceAll("%port%",PORT));
                        out.flush();
                    
                    } else if (request.startsWith("GET /mpris?next HTTP")) {
                        mpris.next();
                        // Build a simple HTML response
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n";
                        response += "<html><head>";
                        response += "<meta http-equiv = \"refresh\" content = \"1; url = http://%host%:%port%/mpris\" />"; //TODO: change URL
                        response += """
                                    <style>
                                    html{
                                        background-color: #000b0c;
                                        color: #00d2e6;
                                        font-family: monospace;
                                    }
                                    </style>
                                    """;
                        response += "</body></head>";
                        out.print(response.replaceAll("%host%",HOST_ADDRESS).replaceAll("%port%",PORT));
                        out.flush();
                    
                    } else if (request.startsWith("GET /mpris?id HTTP")) {
                        // Build a simple HTML response
                        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n\n";
                        String formerID = "ID-";
                        Metadata meta = mpris.getMetadata();
                        if (meta.getTitle()!=null){
                            formerID+=meta.getTitle();
                        }
                        if (meta.getArtist()!=null){
                            formerID+=meta.getArtist();
                        }
                        try {
                            byte[] data = formerID.getBytes();
                            byte[] hash;
                            hash = MessageDigest.getInstance("MD5").digest(data);
                            String checksum = new BigInteger(1, hash).toString(16);
                            response+=checksum;
                        } catch (NoSuchAlgorithmException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            response+="error";
                        }
                        out.print(response);
                        out.flush();
                    
                    } else {
                        // Handle unsupported requests with a 404 response
                        String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                        out.print(response);
                        out.flush();
                    }
                } else {
                    // Respond with a 403 Forbidden error for unauthorized requests
                    String response = "HTTP/1.1 403 Forbidden\r\n\r\n";
                    out.print(response);
                    out.flush();
                }

                // Close streams and socket
                in.close();
                out.close();
                clientSocket.close();
                //System.out.println("..request finished!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Map<String, String> parsePostData(String postData) {
            Map<String, String> data = new HashMap<>();
            String[] keyValuePairs = postData.split("&");
            for (String pair : keyValuePairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8");
                        data.put(key, value);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
            return data;
        }
        private Boolean isIPAddressApproved(String addr){
            //System.out.println(addr);
            for (String approvedIP : APPROVED_IP_ADDRESSES) {
                if (addr.contains(approvedIP)) {
                    return true;
                }
            }
            return false;
        }
    }
}
