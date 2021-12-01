A 2APL Java library for step-based simulations

This library allows you to program cogntive / BDI agents to use in (social) simulation environments. With this version of 2APL, agents are synchronized through ticks, and perform one deliberation cycle (sense-reason-act) per tick. Where the default 2APL allows agents to perform actions in the environment directly, Sim2APl requires agents to produce action references. The actions should be effected in the environment after a tick has executed in a deterministic manner. All this together allows running reproducible, deterministic simulations using complex BDI agents.

### Defaults
By default, agents are initiated through a `Platform`. The platform is the central class through which agents can be instantiated, changed, or removed.

The agents are scheduled and executed through a `TickExecutor`, which ensures all agents scheduled to perform during a tick receive CPU time and collects all produced actions. The default `TickExecutor` uses a `DefaulThreadPoolExecutor` service to run all agents in parallel during a single tick. 

The `TickExecutor` only performs a tick when an outside event is generated. By default, a `SimulationEngine` can generate these events. The `DefaultSimulationEngine` runs each tick in a blocking manner, and notifies subscribed classes through pre- and post tick hooks of scheduling and results. These hooks can be used by the environment to effect the actions produced by the agents in the environment, before the simulation continues with the next step. 

For the environment to register to these hooks, it should implement the `TickHookProcessor` interface

### Open issues
The default messenger is the ACLMessenger, which is able to send messages to other machines through a TCP connection. In the future, messages should be handled by the TickExecutor, as messenges count as an external action. Receiving a message between ticks may influence the outcome of the simulation.

# Installation
This library can be used by other Java programs to program 2APL agents and execute them in a synchronized, tick-based mannaer.

To install, clone the project, and run Maven:

```bash
$ git clone https://github.com/A-Practical-Agent-Programming-Language/Sim2APL.git
$ cd sim2apl
$ mvn install
```

This adds Sim2APL to your local Maven repository. You can now include it as a dependency in your pom.xml, or include the generated Jar under `target` in the included sources of your IDE project.


# License
This library contains free software; The code can be freely used under the Mozilla Public License 2.0. See the license file for details.
This code comes with ABSOLUTELY NO WARRANTY, to the extent permitted by applicable law.
