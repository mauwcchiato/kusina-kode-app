1.26 vs 1.27 on the vivo V17 Pro, both builds compiled with `cmd package compile -m speed -f`, same script.
One run per build. The phone was warm (41 C) and charging, and the same code varied widely between runs
(Pantry 6% vs 67% janky), so these are inconclusive. The Game Map stayed 94-95% janky on every build:
its main cost is not the pin or level-dot animations changed in 1.27 and needs a profiling trace.
