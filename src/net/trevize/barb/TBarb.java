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
import org.apache.commons.cli.UnrecognizedOptionException;

public class TBarb implements Callable<Integer> {
  
  private Options       fCommandLineOptions         = null;
  
  private HelpFormatter fHelpFormatter              = null;
  private String        fHelpCommandLineSyntax      = "barb [OPTION]... <PATTERN_FILE> <REPLACEMENT_FILE> <TARGET_FILE>...";
  
  private String        fHelpHeader                 = "Search and replace text with support of multi-lines pattern (literal or PCRE regular expression).\n\n" 
      + "If a regular expression is provided then:\n" 
      + "  - the MULTILINE flag is used by default, see <https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#MULTILINE>.\n" 
      + "  - the DOTALL flag is used by default, see <https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#DOTALL>.\n\n" + "Options:";
  
  private boolean        fShowHelp                   = false;
  private boolean        fPatternIsRegularExpression = false;
  private boolean        fDisableFlags               = false;
  private boolean        fVerboseOutput              = false;
  private File           fPatternFile                = null;
  private File           fReplacementFile            = null;
  private List<File>     fTargetFileList             = null;
  
  private String         fErrorMessage               = null;
  
  public TBarb() {
    fCommandLineOptions = new Options();
    fCommandLineOptions.addOption( "h", false, "Help" );
    fCommandLineOptions.addOption( "r", false, "Use PATTERN_FILE as a regular expression instead of a literal." );
    fCommandLineOptions.addOption( "u", false, "Disable default flags (DOTALL and MULTILINE). They can be individually enabled in the expression with (?s) for DOTALL, and (?m) for MULTILINE." );
    fCommandLineOptions.addOption( "v", false, "Verbose output: print matches and full error traces." );
    
    fHelpFormatter = new HelpFormatter();
    fHelpFormatter.setSyntaxPrefix( "Usage: " );
  }
  
  public boolean isShowHelp() {
    return fShowHelp;
  }
  
  public boolean isVerboseDebug() {
    return fVerboseOutput;
  }
  
  public String getErrorMessage() {
    return fErrorMessage;
  }
  
  private void printHelp() {
    fHelpFormatter.printHelp( fHelpCommandLineSyntax, fHelpHeader, fCommandLineOptions, null );
  }
  
  private void readCommandLine( String[] aParameterArray ) throws Exception {
    CommandLineParser l_commandLineParser = new DefaultParser();
    CommandLine l_commandLine = l_commandLineParser.parse( fCommandLineOptions, aParameterArray );
    
    if ( l_commandLine.hasOption( "h" ) ) {
      fShowHelp = true;
      return;
    }
    
    if ( l_commandLine.hasOption( "r" ) ) {
      fPatternIsRegularExpression = true;
    }
    
    if ( l_commandLine.hasOption( "u" ) ) {
      fDisableFlags = true;
    }
    
    if ( l_commandLine.hasOption( "v" ) ) {
      fVerboseOutput = true;
    }
    
    List<String> l_argList = l_commandLine.getArgList();
    
    if ( l_argList.size() < 3 ) {
      fErrorMessage = "You must specify files.\nTry 'barb -h' for more information.";
      throw new Exception();
    }
    
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
      if ( ! l_targetFile.isFile() ) {
        System.err.println( String.format( "TargetFile[%s] is not a file, skipping it!", l_targetFile ) );
        continue;
      }
      fTargetFileList.add( l_targetFile );
    }
    
    return;
  }
  
  public void searchAndReplaceByLiteral( String aPattern, String aReplacement, List<File> aTargetFileList ) throws Exception {
    for ( File l_targetFile : aTargetFileList ) {
      try {
        String l_fileContent = new String( Files.readAllBytes( l_targetFile.toPath() ) );
        if ( l_fileContent.contains( aPattern ) ) {
          System.out.println( String.format( "FoundMatch[%b] TargetFile[%s]", true, l_targetFile.getCanonicalPath() ) );
          String l_replacedFileContent = l_fileContent.replace( aPattern, aReplacement );
          Files.write( l_targetFile.toPath(), l_replacedFileContent.getBytes() );
        }
        else if ( fVerboseOutput ) {
          System.out.println( String.format( "FoundMatch[%b] TargetFile[%s]", false, l_targetFile.getCanonicalPath() ) );
        }
      }
      catch ( Exception l_exception ) {
        fErrorMessage = String.format( "Cannot apply replacement in TargetFile[%s]", l_targetFile.getCanonicalPath() );
        throw l_exception;
      }
    }
  }
  
  public void searchAndReplaceByRegex( String aPattern, String aReplacement, List<File> aTargetFileList ) throws Exception {
    Pattern l_regexPattern = null;
    try {
      if ( ! fDisableFlags ) {
        /*
         * DOTALL mode:  In dotall mode, the expression . matches any character, including a line terminator.
         * By default this expression does not match line terminators.
         * Dotall mode can also be enabled via the embedded flag expression (?s). 
         * (The s is a mnemonic for "single-line" mode, which is what this is called in Perl.) 
         */
        
        /*
         * MULTILINE mode: the expressions ^ and $ match just after or just before, respectively, a line terminator or the end of the input sequence.
         * By default these expressions only match at the beginning and the end of the entire input sequence.
         * Multiline mode can also be enabled via the embedded flag expression (?m).
         */
        l_regexPattern = Pattern.compile( aPattern, Pattern.DOTALL | Pattern.MULTILINE );
      }
      else {
        l_regexPattern = Pattern.compile( aPattern );
      }
    }
    catch ( Exception l_exception ) {
      fErrorMessage = String.format( "Cannot build a regex with Pattern[%s]", aPattern );
      throw l_exception;
    }
    for ( File l_targetFile : aTargetFileList ) {
      try {
        String l_fileContent = new String( Files.readAllBytes( l_targetFile.toPath() ) );
        Matcher l_matcher = l_regexPattern.matcher( l_fileContent );
        boolean l_foundMatch = l_matcher.find();
        if ( l_foundMatch ) {
          System.out.println( String.format( "FoundMatch[%b] TargetFile[%s]", l_foundMatch, l_targetFile.getCanonicalPath() ) );
          String l_replacedFileContent = l_matcher.replaceAll( Matcher.quoteReplacement( aReplacement ) );
          Files.write( l_targetFile.toPath(), l_replacedFileContent.toString().getBytes() );
        }
        else if ( fVerboseOutput ) {
          System.out.println( String.format( "FoundMatch[%b] TargetFile[%s]", l_foundMatch, l_targetFile.getCanonicalPath() ) );
        }
      }
      catch ( Exception l_exception ) {
        fErrorMessage = String.format( "Cannot apply replacement in TargetFile[%s]", l_targetFile.getCanonicalPath() );
        throw l_exception;
      }
    }
  }
  
  @Override
  public Integer call() throws Exception {
    String l_patternAsString = null;
    try {
      l_patternAsString = new String( Files.readAllBytes( fPatternFile.toPath() ) );
    }
    catch ( Exception l_exception ) {
      throw new Exception( "Cannot read PatternFile" );
    }
    
    String l_replacementAsString = null;
    try {
      l_replacementAsString = new String( Files.readAllBytes( fReplacementFile.toPath() ) );
    }
    catch ( Exception l_exception ) {
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
    int l_exitCode = 0;
    TBarb l_barb = new TBarb();
    try {
      l_barb.readCommandLine( args );
      if ( l_barb.isShowHelp() ) {
        l_barb.printHelp();
      }
      else {
        l_exitCode = l_barb.call();
      }
    }
    catch ( Exception l_exception ) {
      if ( l_exitCode == 0 ) {
        l_exitCode = 1;
      }
      // String.isBlank() has been introduced in Java11, can't use it below as we compile in java 8.
      if ( ( l_barb.getErrorMessage() != null ) && ( ! l_barb.getErrorMessage().chars().allMatch( Character::isWhitespace ) ) ) {
        System.err.println( l_barb.getErrorMessage() );
      }
      else {
        System.err.println( l_exception.getMessage() );
      }
      if ( l_barb.isVerboseDebug() ) {
        l_exception.printStackTrace();
      }
    }
    System.exit( l_exitCode );
  }
  
}
