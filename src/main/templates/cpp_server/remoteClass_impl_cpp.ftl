${remoteClass.name}Impl.cpp
#include <gst/gst.h>
<#list remoteClassDependencies(remoteClass) as dependency>
<#if model.remoteClasses?seq_contains(dependency)>
#include "${dependency.name}Impl.hpp"
<#else>
#include "${dependency.name}.hpp"
</#if>
</#list>
<#if remoteClass.constructor??>
#include <${remoteClass.name}ImplFactory.hpp>
</#if>
#include "${remoteClass.name}Impl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_${camelToUnderscore(remoteClass.name)?lower_case}_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "Kurento${remoteClass.name}Impl"

namespace kurento
{

<#if remoteClass.constructor??>
${remoteClass.name}Impl::${remoteClass.name}Impl (<#rt>
     <#lt><#list remoteClass.constructor.params as param><#rt>
        <#lt>${getCppObjectType(param.type, true)}${param.name}<#rt>
        <#lt><#if param_has_next>, </#if><#rt>
     <#lt></#list>)<#if remoteClass.extends??> : ${remoteClass.extends.name}Impl (/* FIXME: Add parent class constructor params here */)</#if>
<#else>
${remoteClass.name}Impl::${remoteClass.name}Impl ()
</#if>
{
  // FIXME: Implement this
}
<#list remoteClass.properties as property>

${getCppObjectType (property.type, false)} ${remoteClass.name}Impl::get${property.name?cap_first} ()
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "${remoteClass.name}Impl::get${property.name}: Not implemented");
}
<#if !property.final && !property.readOnly>

void ${remoteClass.name}Impl::set${property.name?cap_first} (${getCppObjectType (property.type, true)}${property.name})
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "${remoteClass.name}Impl::set${property.name}: Not implemented");
}
</#if>
</#list>
<#macro methodStub method>

${getCppObjectType(method.return,false)} ${remoteClass.name}Impl::${method.name} (<#rt>
    <#lt><#list method.params as param>${getCppObjectType(param.type)}${param.name}<#if param_has_next>, </#if></#list>)
{
  // FIXME: Implement this
  throw KurentoException (NOT_IMPLEMENTED, "${remoteClass.name}Impl::${method.name}: Not implemented");
}
</#macro>
<#list remoteClass.methods as method><#rt>
  <#list method.expandIfOpsParams() as expandedMethod ><#rt>
    <#lt><@methodStub expandedMethod />
  </#list>
  <#lt><@methodStub method />
</#list>

<#if remoteClass.constructor??><#rt>
MediaObjectImpl *
${remoteClass.name}ImplFactory::createObject (<#rt>
     <#lt><#list remoteClass.constructor.params as param><#rt>
        <#lt>${getCppObjectType(param.type, true)}${param.name}<#rt>
        <#lt><#if param_has_next>, </#if><#rt>
     <#lt></#list>) const
{
  return new ${remoteClass.name}Impl (<#rt>
     <#lt><#list remoteClass.constructor.params as param><#rt>
        <#lt>${param.name}<#rt>
        <#lt><#if param_has_next>, </#if><#rt>
     <#lt></#list>);
}

</#if>
${remoteClass.name}Impl::StaticConstructor ${remoteClass.name}Impl::staticConstructor;

${remoteClass.name}Impl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
