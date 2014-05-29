package com.weisong.test.verticle.device;

import lombok.Getter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Getter
public class WebsocketDeviceOptions {

    final static private String H = "h";
    final static private String O = "o";
    final static private String P = "p";
    final static private String C = "c";
    
    private int connections = 1;
    private String host = "localhost";
    private int port = 8090;

    private Options options = new Options();

    public WebsocketDeviceOptions(String args[]) throws Exception {
        options.addOption(new Option(H, "help", false, "print this message"));
        options.addOption(new Option(O, "host", true, "The host to connect to, default: localhost"));
        options.addOption(new Option(P, "port", true, "The port to connect to, default: 8090"));
        options.addOption(new Option(C, "count", true, "Number of connections, default: 1"));
        parse(args);
    }

    private void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("device.sh [options]", options);
        System.out.println("Example(s):");
        System.out.println("    device.sh -o localhost -p 8090 -c 5");
        System.out.println();
        System.exit(0);
    }

    private void parse(String args[]) throws ParseException {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(options, args);
        if(cl.hasOption(H)) {
            printHelpAndExit();
        }

        if(cl.hasOption(O)) {
            host = cl.getOptionValue(O);
        }
        
        if(cl.hasOption(P)) {
            port = Integer.valueOf(cl.getOptionValue(P));
        }
        
        if(cl.hasOption(C)) {
            connections = Integer.valueOf(cl.getOptionValue(C));
        }
        
    }
}
