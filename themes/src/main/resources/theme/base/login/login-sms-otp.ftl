<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayWide=false; section>
    <#if section = "header">
        ${msg("authCodePhoneNumber")}
    <#elseif section = "form">

        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" action="${url.loginAction}" method="post">
                    <div class="${properties.kcFormGroupClass!} row">
                        <label for="code" class="${properties.kcLabelClass!}">${msg("authenticationCode")}</label>
                        <div id="otp-container">
                            <input v-for="(digit, index) in codeDigits" :key="index" v-model="codeDigits[index]"
                                   class="otp-input" type="text" maxlength="1"
                                   @input="moveToNext(index)" @keydown.backspace="moveToPrev(index)"
                                   @paste="handlePaste($event)">
                        </div>
                        <input type="hidden" id="code" name="code" v-model="codeValue"/>
                    </div>
                    <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                        <input type="hidden" id="id-hidden-input" name="credentialId"
                               <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                        <input tabindex="2"
                               class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="save" id="kc-login" type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </form>
            </div>
        </div>
    <#elseif section = "info">
        ${msg("authCodeInfo")}
    </#if>
</@layout.registrationLayout>

<style>
    #otp-container {
        display: flex;
        gap: 8px;
    }
    .otp-input {
        width: 16.6%;
        height: 40px;
        text-align: center;
        font-size: 20px;
        border: 2px solid #ccc;
        border-radius: 5px;
        outline: none;
        transition: border-color 0.2s;
    }
    .otp-input:focus {
        border-color: #007bff;
    }
</style>

<script type="text/javascript" src="${url.resourcesPath}/js/vue.min.js"></script>
<script>
    new Vue({
        el: '#kc-form-login',
        data() {
            return {
                codeDigits: ["", "", "", "", "", ""], // 6个输入框
            };
        },
        computed: {
            codeValue() {
                return this.codeDigits.join(""); // 组合成完整验证码
            }
        },
        methods: {
            moveToNext(index) {
                if (this.codeDigits[index] && index < this.codeDigits.length - 1) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index + 1].focus();
                    });
                }
            },
            moveToPrev(index) {
                if (!this.codeDigits[index] && index > 0) {
                    this.$nextTick(() => {
                        this.$el.querySelectorAll('.otp-input')[index - 1].focus();
                    });
                }
            },
            handlePaste(event) {
                event.preventDefault();
                const pastedData = event.clipboardData.getData('text').slice(0, 6).split('');
                for (let i = 0; i < pastedData.length; i++) {
                    this.codeDigits[i] = pastedData[i];
                }
            }
        }
    });
</script>
