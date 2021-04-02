# PhD-Mobile-K8s-HPC-Platform

When solving compute-intensive tasks in a distributed and parallel way, x86 hardware resources (CPU / GPU) and specialized infrastructure (Grid, Cluster, Cloud) are commonly used to achieve high performance. In its early days, x86 processors, co-processors, and chips were developed to solve complex problems regardless of their power consumption.
Due to its direct impact on costs and the environment, optimizing use, cooling and energy consumption, as well as analyzing alternative architectures, became a main concern of organizations. As a result, companies and institutions have proposed different infrastructures to implement scalability, flexibility, and concurrency features.
To propose an alternative architecture to traditional schemes, this thesis aims to execute the processing tasks by reusing the idle capacities of mobile devices. These gadgets integrate ARM processors which, in contrast to traditional x86 architectures, were developed with energy efficiency as their cornerstone, since they are mostly powered by batteries. These devices, in recent years, have increased their capacity, efficiency, stability, processing power, as well as massiveness and market; while maintaining a low price, size and energy consumption. At the same time, they face idle periods during recharging battery lapses, which represents great potential that can be reused.
To properly manage and exploit these resources, and turn them into a processing-intensive data center; A distributed, collaborative, elastic and low-cost platform was designed, developed and evaluated based on microservices and containers orchestrated (Kubernetes) in Cloud and local environments, integrated with DevOps tools, methodologies and practices architecture.
The microservices paradigm allowed the developed features to be fragmented into smaller services, with limited responsibilities. DevOps practices facilitate the building of automated processes for the execution of tests, traceability, monitoring and integration for changes and new features and improvements. Finally, packaging the features including all their dependencies and libraries in containers helped to keep services small, immutable, portable, secure and standardized that allow their execution independent of the underlying architecture. Including Kubernetes as a Container Orchestrator, allowed services to be managed, deployed and scaled in a comprehensive and transparent way, both locally and in the Cloud, ensuring efficient usage of the infrastructure, costs and energy.
To validate the system’s performance, scalability, power consumption and flexibility, several concurrent video transcoding scenarios were run. On the one hand, it was possible to test the behaviour and performance of various mobile and x86 devices executing different stress conditions. On the other hand, it was possible to show how the architecture was flexibilized and scaled to face the processing needs.
The experimental results, based on the performance, load and saturation scenarios proposed, show that useful improvements are obtained following the baseline of this study and that the architecture developed is robust enough to be considered as a scalable, elastic, and cheaper alternative compared to traditional models.
