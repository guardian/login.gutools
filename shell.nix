{ sources ? import ./nix/sources.nix }:
let
  pkgs = import sources.nixpkgs { };
  guardianNix = builtins.fetchGit {
    url = "git@github.com:guardian/guardian-nix.git";
    ref = "refs/tags/v1";
  };
  guardianDev = import "${guardianNix.outPath}/guardian-dev.nix" pkgs;

  sbt = pkgs.sbt.override { jre = pkgs.corretto17; };
  metals = pkgs.metals;

  sbtRun = pkgs.writeShellApplication {
    name = "sbt-run";
    runtimeInputs = [ sbt ];
    text = ''
      sbt run
    '';
  };

  maybeRunNginx = pkgs.writeShellApplication {
    name = "maybe-run-nginx";
    runtimeInputs = [ ];
    text = ''
      if pgrep nginx >/dev/null; then
        echo "nginx is already running"
      else
        echo "nginx isn't running, booting now..."
        dev-nginx restart
      fi
    '';
  };

in guardianDev.devEnv {
  name = "login.gutools";
  commands = [ sbtRun maybeRunNginx ];
  extraInputs = [ metals sbt pkgs.scala_2_13 ];
}
