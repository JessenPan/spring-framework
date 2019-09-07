/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.quartz;

import org.quartz.JobDataMap;
import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Subclass of {@link AdaptableJobFactory} that also supports Spring-style
 * dependency injection on bean properties. This is essentially the direct
 * equivalent of Spring's {@link QuartzJobBean} in the shape of a
 * Quartz 1.5 {@link org.quartz.spi.JobFactory}.
 * <p>
 * <p>Applies scheduler context, job data map and trigger data map entries
 * as bean property values. If no matching bean property is found, the entry
 * is by default simply ignored. This is analogous to QuartzJobBean's behavior.
 * <p>
 * <p>Compatible with Quartz 1.5+ as well as Quartz 2.0-2.2, as of Spring 3.2.
 *
 * @author Juergen Hoeller
 * @see SchedulerFactoryBean#setJobFactory
 * @see QuartzJobBean
 * @since 2.0
 */
public class SpringBeanJobFactory extends AdaptableJobFactory implements SchedulerContextAware {

    private String[] ignoredUnknownProperties;

    private SchedulerContext schedulerContext;


    /**
     * Specify the unknown properties (not found in the bean) that should be ignored.
     * <p>Default is {@code null}, indicating that all unknown properties
     * should be ignored. Specify an empty array to throw an exception in case
     * of any unknown properties, or a list of property names that should be
     * ignored if there is no corresponding property found on the particular
     * job class (all other unknown properties will still trigger an exception).
     */
    public void setIgnoredUnknownProperties(String... ignoredUnknownProperties) {
        this.ignoredUnknownProperties = ignoredUnknownProperties;
    }

    public void setSchedulerContext(SchedulerContext schedulerContext) {
        this.schedulerContext = schedulerContext;
    }


    /**
     * Create the job instance, populating it with property values taken
     * from the scheduler context, job data map and trigger data map.
     */
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
        if (isEligibleForPropertyPopulation(bw.getWrappedInstance())) {
            MutablePropertyValues pvs = new MutablePropertyValues();
            if (this.schedulerContext != null) {
                pvs.addPropertyValues(this.schedulerContext);
            }
            pvs.addPropertyValues(getJobDetailDataMap(bundle));
            pvs.addPropertyValues(getTriggerDataMap(bundle));
            if (this.ignoredUnknownProperties != null) {
                for (String propName : this.ignoredUnknownProperties) {
                    if (pvs.contains(propName) && !bw.isWritableProperty(propName)) {
                        pvs.removePropertyValue(propName);
                    }
                }
                bw.setPropertyValues(pvs);
            } else {
                bw.setPropertyValues(pvs, true);
            }
        }
        return job;
    }

    /**
     * Return whether the given job object is eligible for having
     * its bean properties populated.
     * <p>The default implementation ignores {@link QuartzJobBean} instances,
     * which will inject bean properties themselves.
     *
     * @param jobObject the job object to introspect
     * @see QuartzJobBean
     */
    protected boolean isEligibleForPropertyPopulation(Object jobObject) {
        return (!(jobObject instanceof QuartzJobBean));
    }

    // Reflectively adapting to differences between Quartz 1.x and Quartz 2.0...
    private JobDataMap getJobDetailDataMap(TriggerFiredBundle bundle) throws Exception {
        Method getJobDetail = bundle.getClass().getMethod("getJobDetail");
        Object jobDetail = ReflectionUtils.invokeMethod(getJobDetail, bundle);
        Method getJobDataMap = jobDetail.getClass().getMethod("getJobDataMap");
        return (JobDataMap) ReflectionUtils.invokeMethod(getJobDataMap, jobDetail);
    }

    // Reflectively adapting to differences between Quartz 1.x and Quartz 2.0...
    private JobDataMap getTriggerDataMap(TriggerFiredBundle bundle) throws Exception {
        Method getTrigger = bundle.getClass().getMethod("getTrigger");
        Object trigger = ReflectionUtils.invokeMethod(getTrigger, bundle);
        Method getJobDataMap = trigger.getClass().getMethod("getJobDataMap");
        return (JobDataMap) ReflectionUtils.invokeMethod(getJobDataMap, trigger);
    }

}
