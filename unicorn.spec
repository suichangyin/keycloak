Name:           unicorn2
Version:        1.0
Release:        1%{?dist}
Summary:        Unicorn build with Quarkus package

License:        SMT
Source0:        %{name}-%{version}.tar.gz

BuildRequires:  maven, java-11-openjdk, tar
Requires:       java-11-openjdk

Conflicts:      unicorn

BuildArch:      noarch

%description
This is a new unicorn Quarkus package built with Maven.

%prep
%setup -q

%build
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac))))
mvn -pl quarkus/deployment,quarkus/dist -am -DskipTests -Dmaven.build.cache.enabled=false clean install

mkdir -p %{_builddir}/tar_output
cp target/%{name}-%{version}.tar.gz %{_builddir}/tar_output/

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt/%{name}

tar -xzf %{_builddir}/tar_output/%{name}-%{version}.tar.gz -C %{buildroot}/opt/%{name}/

%files
/opt/%{name}/

%changelog
* Thu Feb 27 2025 JieQiang Sui <sui.jieqiang@datatom.com> - 1.0-1
- Initial unicorn2 package