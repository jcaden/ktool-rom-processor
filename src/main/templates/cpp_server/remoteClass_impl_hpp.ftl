${remoteClass.name}Impl.hpp
#ifndef __${camelToUnderscore(remoteClass.name)}_IMPL_HPP__
#define __${camelToUnderscore(remoteClass.name)}_IMPL_HPP__

<#if (remoteClass.extends)??>
#include "${remoteClass.extends.name}Impl.hpp"
<#else>
#include <Factory.hpp>
</#if>
#include "${remoteClass.name}.hpp"
#include <EventHandler.hpp>

namespace kurento
{

<#list remoteClassDependencies(remoteClass) as dependency>
<#if model.remoteClasses?seq_contains(dependency)>
class ${dependency.name}Impl;
<#else>
class ${dependency.name};
</#if>
</#list>
class ${remoteClass.name}Impl;

void Serialize (std::shared_ptr<${remoteClass.name}Impl> &object, JsonSerializer &serializer);

class ${remoteClass.name}Impl :<#if remoteClass.extends??><#rt>
   <#lt> public ${remoteClass.extends.name}Impl<#rt>,
   </#if><#lt> public virtual ${remoteClass.name}
{

public:

<#if remoteClass.constructor?? >
  ${remoteClass.name}Impl (<#rt>
     <#lt><#list remoteClass.constructor.params as param><#rt>
        <#lt>${getCppObjectType(param.type, true)}${param.name}<#rt>
        <#lt><#if param_has_next>, </#if><#rt>
     <#lt></#list>);
<#else>
  ${remoteClass.name}Impl ();
</#if>

  virtual ~${remoteClass.name}Impl () {};
  <#macro methodHeader method>
  ${getCppObjectType(method.return,false)} ${method.name} (<#rt>
      <#lt><#list method.params as param>${getCppObjectType(param.type)}${param.name}<#if param_has_next>, </#if></#list>);
  </#macro>
  <#list remoteClass.methods as method><#rt>
    <#if method_index = 0 >

    </#if>
    <#list method.expandIfOpsParams() as expandedMethod ><#rt>
      <#lt><@methodHeader expandedMethod />
    </#list>
    <#lt><@methodHeader method />
  </#list>
<#list remoteClass.properties as property>

  virtual ${getCppObjectType (property.type, false)} get${property.name?cap_first} ();
  <#if !property.final && !property.readOnly>
  virtual void set${property.name?cap_first} (${getCppObjectType (property.type, true)}${property.name});
  </#if>
</#list>

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType, std::shared_ptr<EventHandler> handler);
  <#list remoteClass.events as event>
    <#if event_index = 0 >

    </#if>
  sigc::signal<void, ${event.name}> signal${event.name};
  </#list>

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

private:

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

};

} /* kurento */

#endif /*  __${camelToUnderscore(remoteClass.name)}_IMPL_HPP__ */
