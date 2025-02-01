package net.trevize.barb;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class TBarb implements Callable<Integer> {
  
  private Options       fOptions                    = null;
  private HelpFormatter fHelpFormatter              = null;
  private String        fHelpCommandLineSyntax      = "barb [OPTION]... <PATTERN_FILE> <REPLACEMENT_FILE> <TARGET_FILE>...";
  private String        fHelpHeader                 = "Search and replace text with support of multi-lines pattern (literal or regular expression).\n\nOptions:\n";
  
  private boolean       fPatternIsRegularExpression = false;
  private boolean       fVerboseOutput              = false;
  private File          fPatternFile                = null;
  private File          fReplacementFile            = null;
  private List<File>    fTargetFileList             = null;
  
  public TBarb() {
    fOptions = new Options();
    fOptions.addOption( "r", false, "Use PATTERN_FILE as a regular expression instead of a literal" );
    fOptions.addOption( "v", false, "Verbose output" );
    
    fHelpFormatter = new HelpFormatter();
    fHelpFormatter.setSyntaxPrefix( "Usage: " );
  }
  
  public boolean isVerboseDebug() {
    return fVerboseOutput;
  }
  
  private void printHelp() {
    fHelpFormatter.printHelp( fHelpCommandLineSyntax, fHelpHeader, fOptions, null );
  }
  
  private void readCommandLine( String[] aParameterArray ) throws Exception {
    if ( ( aParameterArray == null ) || ( aParameterArray.length < 3 ) ) {
      System.err.println( "Wrong number of parameters" );
      throw new Exception( "Wrong number of parameters" );
    }
    
    CommandLineParser l_commandLineParser = new DefaultParser();
    CommandLine l_commandLine = l_commandLineParser.parse( fOptions, aParameterArray );
    
    if ( l_commandLine.hasOption( "r" ) ) {
      fPatternIsRegularExpression = true;
    }
    
    if ( l_commandLine.hasOption( "v" ) ) {
      fVerboseOutput = true;
    }
    
    List<String> l_argList = l_commandLine.getArgList();
    
    File l_patternFile = new File( l_argList.get( 0 ) );
    if ( ! l_patternFile.isFile() ) {
      throw new Exception( "Cannot find PatternFile" );
    }
    fPatternFile = l_patternFile;
    
    File l_replacementFile = new File( l_argList.get( 1 ) );
    if ( ! l_replacementFile.isFile() ) {
      throw new Exception( "Cannot find ReplacementFile" );
    }
    fReplacementFile = l_replacementFile;
    
    fTargetFileList = new ArrayList<>();
    for ( int l_targetFilePathIndex = 2; l_targetFilePathIndex < l_argList.size(); ++l_targetFilePathIndex ) {
      String l_targetFilePath = l_argList.get( l_targetFilePathIndex );
      File l_targetFile = new File( l_targetFilePath );
      fTargetFileList.add( l_targetFile );
    }
  }
  
  public void searchAndReplaceByLiteral( String aPattern, String aReplacement, List<File> aTargetFileList ) throws Exception {
    for ( File l_targetFile : aTargetFileList ) {
      try {
        String l_fileContent = new String( Files.readAllBytes( l_targetFile.toPath() ) );
        if ( l_fileContent.contains( aPattern ) ) {
          System.err.println( String.format( "FoundMatch[%b] TargetFile[%s]", true, l_targetFile.getCanonicalPath() ) );
          String l_replacedFileContent = l_fileContent.replaceFirst( aPattern, aReplacement );
          Files.write( l_targetFile.toPath(), l_replacedFileContent.getBytes() );
        }
        else if ( fVerboseOutput ) {
          System.err.println( String.format( "FoundMatch[%b] TargetFile[%s]", false, l_targetFile.getCanonicalPath() ) );
        }
      }
      catch ( Exception aException ) {
        System.err.println( String.format( "Cannot apply replacement in TargetFile[%s]", l_targetFile.getCanonicalPath() ) );
        throw aException;
      }
    }
  }
  
  public void searchAndReplaceByRegex( String aPattern, String aReplacement, List<File> aTargetFileList ) throws Exception {
    Pattern l_regexPattern = null;
    try {
      l_regexPattern = Pattern.compile( aPattern );
    }
    catch ( Exception aException ) {
      System.err.println( String.format( "Cannot build a regex with Pattern[%s]", aPattern ) );
      throw aException;
    }
    for ( File l_targetFile : aTargetFileList ) {
      try {
        String l_fileContent = new String( Files.readAllBytes( l_targetFile.toPath() ) );
        Matcher l_matcher = l_regexPattern.matcher( l_fileContent );
        boolean l_foundMatch = l_matcher.find();
        if ( l_foundMatch ) {
          System.err.println( String.format( "FoundMatch[%b] TargetFile[%s]", l_foundMatch, l_targetFile.getCanonicalPath() ) );
          String l_replacedFileContent = l_matcher.replaceAll( Matcher.quoteReplacement( aReplacement ) );
          Files.write( l_targetFile.toPath(), l_replacedFileContent.toString().getBytes() );
        }
        else if ( fVerboseOutput ) {
          System.err.println( String.format( "FoundMatch[%b] TargetFile[%s]", l_foundMatch, l_targetFile.getCanonicalPath() ) );
        }
      }
      catch ( Exception aException ) {
        System.err.println( String.format( "Cannot apply replacement in TargetFile[%s]", l_targetFile.getCanonicalPath() ) );
        throw aException;
      }
    }
  }
  
  @Override
  public Integer call() throws Exception {
    String l_patternAsString = null;
    try {
      l_patternAsString = new String( Files.readAllBytes( fPatternFile.toPath() ) );
    }
    catch ( Exception aException ) {
      throw new Exception( "Cannot read PatternFile" );
    }
    
    String l_replacementAsString = null;
    try {
      l_replacementAsString = new String( Files.readAllBytes( fReplacementFile.toPath() ) );
    }
    catch ( Exception aException ) {
      throw new Exception( "Cannot read ReplacementFile" );
    }
    
    if ( ! fPatternIsRegularExpression ) {
      searchAndReplaceByLiteral( l_patternAsString, l_replacementAsString, fTargetFileList );
    }
    else {
      searchAndReplaceByRegex( l_patternAsString, l_replacementAsString, fTargetFileList );
    }
    return 0;
  }
  
  public static void main( String[] args ) {
    int l_exitCode = 1;
    TBarb l_barb = new TBarb();
    try {
      l_barb.readCommandLine( args );
      l_exitCode = l_barb.call();
    }
    catch ( Exception aException ) {
      if ( l_exitCode != 0 ) {
        System.err.println( "Error\n" );
      }
      if ( l_barb.isVerboseDebug() ) {
        aException.printStackTrace();
      }
      System.err.println( "\n" );
      l_barb.printHelp();
    }
    System.exit( l_exitCode );
  }
  
}
