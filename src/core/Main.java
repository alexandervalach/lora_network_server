package core;

public class Main {
  private static ProgramResources resources;

  public static void main(String[] args) {
    // Run with false as normal server, run with true as in debug mode
    resources = new ProgramResources(false);
  }
}
